package com.risk.service;

import com.risk.engine.RiskIndicators;
import com.risk.engine.ScoreCardEngine;
import com.risk.dto.RiskScoreResult;
import com.risk.entity.CountryRiskConfig;
import com.risk.entity.Enterprise;
import com.risk.entity.RiskScore;
import com.risk.entity.WarningMessage;
import com.risk.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class EnterpriseService {

    @Autowired private EnterpriseMapper enterpriseMapper;
    @Autowired private AbnormalOperationMapper abnormalOperationMapper;
    @Autowired private AdministrativePenaltyMapper penaltyMapper;
    @Autowired private CustomsDeclarationMapper customsMapper;
    @Autowired private ForeignTradeRecordMapper tradeRecordMapper;
    @Autowired private WarningMessageMapper warningMessageMapper;
    @Autowired private RiskScoreMapper riskScoreMapper;
    @Autowired private CountryRiskConfigMapper countryRiskConfigMapper;

    @Autowired private ScoreCardEngine scoreCardEngine;
    @Autowired private PublicDataClient publicDataClient;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 模糊搜索企业 */
    public List<Enterprise> fuzzySearch(String keyword) {
        return enterpriseMapper.fuzzySearch(keyword);
    }

    /** 查询所有企业 */
    public List<Enterprise> findAll() {
        return enterpriseMapper.findAll();
    }

    /** 按信用代码查询 */
    public Enterprise getByCreditCode(String creditCode) {
        return enterpriseMapper.findByCreditCode(creditCode);
    }

    /** 构建风控指标数据（聚合多源公共数据） */
    public RiskIndicators buildRiskIndicators(String creditCode) {
        Enterprise ent = enterpriseMapper.findByCreditCode(creditCode);
        if (ent == null) return null;

        RiskIndicators ind = new RiskIndicators();
        // 基础信息
        ind.setCreditCode(ent.getCreditCode());
        ind.setEnterpriseName(ent.getEnterpriseName());
        ind.setRegisteredCapital(ent.getRegisteredCapital() != null ? ent.getRegisteredCapital() : 0);
        ind.setEstablishYears(ent.getEstablishYears() != null ? ent.getEstablishYears() : 0);
        ind.setIndustryCategory(ent.getIndustryCategory() != null ? ent.getIndustryCategory() : "");
        ind.setPrimaryPort(ent.getPrimaryPort() != null ? ent.getPrimaryPort() : "");

        // 经营合规
        ind.setAbnormalOperationCount(abnormalOperationMapper.countByCreditCode(creditCode));
        ind.setRecentAbnormalCount(abnormalOperationMapper.countRecentByCreditCode(creditCode));
        ind.setAdministrativePenaltyCount(penaltyMapper.countByCreditCode(creditCode));
        ind.setInDishonestyList(ind.getAbnormalOperationCount() >= 3); // 简化判断

        // 外贸资质
        ind.setHasForeignTradeRecord(tradeRecordMapper.hasForeignTradeRecord(creditCode) > 0);
        ind.setHasCrossBorderEcommerceRecord(tradeRecordMapper.hasCrossBorderEcommerceRecord(creditCode) > 0);

        // 口岸通关
        ind.setCustomsViolationCount(customsMapper.countViolations(creditCode));
        ind.setInspectionAbnormalRate(customsMapper.avgInspectionAbnormalRate(creditCode));

        double avgDuration = customsMapper.avgClearanceDuration(creditCode);
        ind.setCustomsTimeDeviation(avgDuration > 0 ? avgDuration / 10.0 : 1.0); // 10小时为基准

        // 贸易活跃度
        ind.setRecent12MonthDeclarationCount(customsMapper.countRecent12Month(creditCode));
        ind.setTradeCategoryCount(customsMapper.countGoodsCategory(creditCode));
        ind.setContinuousActiveMonths(customsMapper.countContinuousMonths(creditCode));

        return ind;
    }

    /** 构建风控指标数据（带目标国家） */
    public RiskIndicators buildRiskIndicators(String creditCode, String targetCountry) {
        RiskIndicators ind = buildRiskIndicators(creditCode);
        if (ind != null) {
            String normalizedCountry = targetCountry != null ? targetCountry.trim() : "";
            ind.setTargetCountry(normalizedCountry);
            // 评分请求只使用本地已落库指标，避免比赛现场因外部网络不可用而阻塞。
            // portClearanceAvgHours 当前不参与评分公式，保留本地可解释值供后续扩展；
            // borderTradeAmount 没有可靠金额来源时保持为空，不用申报次数伪造金额。
            if (!normalizedCountry.isEmpty()) {
                double avgHours = ind.getCustomsTimeDeviation() > 0
                        ? ind.getCustomsTimeDeviation() * 10.0
                        : 10.0;
                ind.setPortClearanceAvgHours(Math.round(avgHours * 10.0) / 10.0);
                ind.setInspectionFailCount(customsMapper.countViolations(creditCode));
            }
        }
        return ind;
    }

    /** 获取风控评分（带目标国家权重） */
    public RiskScoreResult getRiskScore(String creditCode, String targetCountry) {
        if (targetCountry == null || targetCountry.isEmpty()) {
            return getRiskScore(creditCode);
        }

        // 根据国家名称查找国家代码
        String countryCode = mapCountryNameToCode(targetCountry);
        CountryRiskConfig config = countryRiskConfigMapper.findByCode(countryCode);

        RiskIndicators indicators = buildRiskIndicators(creditCode, targetCountry);
        if (indicators == null) return null;

        if (config != null) {
            Map<String, Double> weights = new HashMap<>();
            weights.put("basic", config.getBasicWeight());
            weights.put("compliance", config.getComplianceWeight());
            weights.put("customs", config.getCustomsWeight());
            weights.put("trade", config.getTradeWeight());
            weights.put("adjust", config.getAdjustWeight());
            return scoreCardEngine.calculateScore(indicators, weights);
        }

        return scoreCardEngine.calculateScore(indicators);
    }

    /** 国家名称映射为国家代码 */
    private String mapCountryNameToCode(String countryName) {
        if (countryName == null) return "CN";
        switch (countryName) {
            case "越南": return "VN";
            case "泰国": return "TH";
            case "马来西亚": return "MY";
            case "印尼": case "印度尼西亚": return "ID";
            case "柬埔寨": return "KH";
            case "老挝": return "LA";
            case "中国": return "CN";
            default: return "CN";
        }
    }

    /** 获取风控评分（带缓存） */
    public RiskScoreResult getRiskScore(String creditCode) {
        // 查缓存
        String cacheKey = "risk:score:" + creditCode;
        if (redisTemplate != null) {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof RiskScoreResult) return (RiskScoreResult) cached;
        }

        RiskIndicators indicators = buildRiskIndicators(creditCode);
        if (indicators == null) return null;

        RiskScoreResult result = scoreCardEngine.calculateScore(indicators);

        // 持久化评分到数据库
        try {
            RiskScore riskScore = new RiskScore();
            riskScore.setCreditCode(creditCode);
            riskScore.setTotalScore(result.getTotalScore());
            riskScore.setRiskLevel(result.getRiskLevel());
            riskScore.setScoreDate(new Date());
            // 构建评分明细JSON
            String detail = String.format(
                "{\"basicScore\":%.2f,\"complianceScore\":%.2f,\"customsScore\":%.2f,\"tradeScore\":%.2f,\"adjustScore\":%.2f}",
                result.getBasicScore() != null ? result.getBasicScore() : 0,
                result.getComplianceScore() != null ? result.getComplianceScore() : 0,
                result.getCustomsScore() != null ? result.getCustomsScore() : 0,
                result.getTradeScore() != null ? result.getTradeScore() : 0,
                result.getAdjustScore() != null ? result.getAdjustScore() : 0
            );
            riskScore.setScoreDetail(detail);
            riskScoreMapper.insert(riskScore);
        } catch (Exception e) {
            // 表不存在或写入失败时不影响主流程
        }

        // 写缓存1小时
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofHours(1));
        }
        return result;
    }

    /** 获取最近预警 */
    public List<WarningMessage> getLatestWarnings(int limit) {
        return warningMessageMapper.findLatest(limit);
    }

    /** 本月预警数 */
    public int countThisMonthWarnings() {
        return warningMessageMapper.countThisMonth();
    }

    /** 企业总数 */
    public int countEnterprises() {
        return enterpriseMapper.countAll();
    }

    /** 高风险企业数（评分<40） */
    public int countHighRisk() {
        // 遍历所有企业计算评分
        List<Enterprise> enterprises = enterpriseMapper.findAll();
        if (enterprises == null || enterprises.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Enterprise ent : enterprises) {
            RiskScoreResult result = getRiskScore(ent.getCreditCode());
            if (result != null && "高风险".equals(result.getRiskLevel())) count++;
        }
        return count;
    }
}
