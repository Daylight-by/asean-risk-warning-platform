package com.risk.engine;

import com.risk.dto.RiskScoreResult;
import com.risk.entity.RiskThresholdConfig;
import com.risk.service.RiskThresholdConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * 风控评分卡引擎 - 核心算法类
 * 采用传统评分卡模型（Scorecard），纯Java实现，无需任何深度学习框架
 * 评分维度：
 *   1. 基础资质（权重20%）
 *   2. 经营合规（权重30%）
 *   3. 口岸通关（权重25%）
 *   4. 贸易活跃（权重15%）
 *   5. 综合调整（权重10%）
 */
@Component
public class ScoreCardEngine {

    // ============ 评分常量 ============
    private static final double BASE_SCORE = 30.0;
    private static final double MAX_SCORE = 100.0;
    private static final double MIN_SCORE = 0.0;

    @Autowired
    private RiskThresholdConfigService thresholdConfigService;

    // ============ 维度权重 ============
    private static final double WEIGHT_BASIC = 0.20;
    private static final double WEIGHT_COMPLIANCE = 0.30;
    private static final double WEIGHT_CUSTOMS = 0.25;
    private static final double WEIGHT_TRADE = 0.15;
    private static final double WEIGHT_ADJUST = 0.10;

    /**
     * 计算企业综合风控评分（分国别自定义权重版本）
     * 支持"一国一策"差异化权重模型
     * @param indicators 风险指标
     * @param countryWeights 国别权重Map，key为维度名(basic/compliance/customs/trade/adjust)，value为权重值
     */
    public RiskScoreResult calculateScore(RiskIndicators indicators, Map<String, Double> countryWeights) {
        double wBasic = countryWeights.getOrDefault("basic", WEIGHT_BASIC);
        double wCompliance = countryWeights.getOrDefault("compliance", WEIGHT_COMPLIANCE);
        double wCustoms = countryWeights.getOrDefault("customs", WEIGHT_CUSTOMS);
        double wTrade = countryWeights.getOrDefault("trade", WEIGHT_TRADE);
        double wAdjust = countryWeights.getOrDefault("adjust", WEIGHT_ADJUST);

        double basicScore = calculateBasicScore(indicators);
        double complianceScore = calculateComplianceScore(indicators);
        double customsScore = calculateCustomsScore(indicators);
        double tradeScore = calculateTradeScore(indicators);
        double adjustScore = calculateAdjustScore(indicators);

        double totalScore = BASE_SCORE
                + (basicScore * wBasic)
                + (complianceScore * wCompliance)
                + (customsScore * wCustoms)
                + (tradeScore * wTrade)
                + (adjustScore * wAdjust);

        int riskCount = 0;
        if (indicators.getAbnormalOperationCount() > 0) riskCount++;
        if (indicators.getAdministrativePenaltyCount() > 0) riskCount++;
        if (indicators.getCustomsViolationCount() > 0) riskCount++;
        if (indicators.isInDishonestyList()) riskCount++;
        if (indicators.getInspectionAbnormalRate() >= 0.10) riskCount++;

        // 分国别额外扣分：查验不合格次数
        if (indicators.getInspectionFailCount() != null && indicators.getInspectionFailCount() > 0) {
            riskCount += Math.min(indicators.getInspectionFailCount(), 3);
        }

        totalScore -= riskCount * 5;
        totalScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, totalScore));
        totalScore = Math.round(totalScore * 100.0) / 100.0;

        RiskThresholdConfig thresholds = thresholdConfigService.getConfig();
        String riskLevel;
        if (totalScore <= thresholds.getHighRiskScore()) {
            riskLevel = "高风险";
        } else if (totalScore <= thresholds.getMidRiskScore()) {
            riskLevel = "中风险";
        } else {
            riskLevel = "低风险";
        }

        List<String> riskTags = generateRiskTags(indicators);
        if (indicators.getTargetCountry() != null && !indicators.getTargetCountry().isEmpty()) {
            riskTags.add("适用" + indicators.getTargetCountry() + "国别风控权重模型");
        }

        RiskScoreResult result = new RiskScoreResult();
        result.setTotalScore(totalScore);
        result.setRiskLevel(riskLevel);
        result.setBasicScore(round(basicScore));
        result.setComplianceScore(round(complianceScore));
        result.setCustomsScore(round(customsScore));
        result.setTradeScore(round(tradeScore));
        result.setAdjustScore(round(adjustScore));
        result.setRiskTags(riskTags);
        result.setCalculateTime(new Date());
        return result;
    }

    /**
     * 计算企业综合风控得分（默认权重版本）
     * 公式: 总分 = BASE_SCORE(30) + 各维度加权贡献
     * 各维度满分100，加权后最大贡献 = 100*(0.2+0.3+0.25+0.15+0.1) = 100
     * 因此理论最高分 = 30 + 100 = 130，经min(100)封顶
     * 无任何风险记录且资质优秀的企业 ≈ 90-100分
     * 有少量风险记录的企业 ≈ 50-75分
     * 有多项风险记录的企业 ≈ 20-45分
     */
    public RiskScoreResult calculateScore(RiskIndicators indicators) {
        // 1. 计算各维度得分
        double basicScore = calculateBasicScore(indicators);
        double complianceScore = calculateComplianceScore(indicators);
        double customsScore = calculateCustomsScore(indicators);
        double tradeScore = calculateTradeScore(indicators);
        double adjustScore = calculateAdjustScore(indicators);

        // 2. 加权综合得分（基础分30 + 各维度加权）
        double totalScore = BASE_SCORE
                + (basicScore * WEIGHT_BASIC)
                + (complianceScore * WEIGHT_COMPLIANCE)
                + (customsScore * WEIGHT_CUSTOMS)
                + (tradeScore * WEIGHT_TRADE)
                + (adjustScore * WEIGHT_ADJUST);

        // 3. 额外惩罚：有多项风险记录时额外扣分
        int riskCount = 0;
        if (indicators.getAbnormalOperationCount() > 0) riskCount++;
        if (indicators.getAdministrativePenaltyCount() > 0) riskCount++;
        if (indicators.getCustomsViolationCount() > 0) riskCount++;
        if (indicators.isInDishonestyList()) riskCount++;
        if (indicators.getInspectionAbnormalRate() >= 0.10) riskCount++;

        // 每多一项风险记录扣5分，最多扣25分
        totalScore -= riskCount * 5;

        // 4. 边界约束
        totalScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, totalScore));
        totalScore = Math.round(totalScore * 100.0) / 100.0;

        // 4. 风险等级
        RiskThresholdConfig thresholds = thresholdConfigService.getConfig();
        String riskLevel;
        if (totalScore <= thresholds.getHighRiskScore()) {
            riskLevel = "高风险";
        } else if (totalScore <= thresholds.getMidRiskScore()) {
            riskLevel = "中风险";
        } else {
            riskLevel = "低风险";
        }

        // 5. 风险标签
        List<String> riskTags = generateRiskTags(indicators);

        // 6. 构建结果
        RiskScoreResult result = new RiskScoreResult();
        result.setTotalScore(totalScore);
        result.setRiskLevel(riskLevel);
        result.setBasicScore(round(basicScore));
        result.setComplianceScore(round(complianceScore));
        result.setCustomsScore(round(customsScore));
        result.setTradeScore(round(tradeScore));
        result.setAdjustScore(round(adjustScore));
        result.setRiskTags(riskTags);
        result.setCalculateTime(new Date());
        return result;
    }

    // ============ 维度一：基础资质（满分100） ============
    private double calculateBasicScore(RiskIndicators ind) {
        double score = 0;
        // 注册资本
        double cap = ind.getRegisteredCapital();
        if (cap >= 1000)      score += 25;
        else if (cap >= 500)  score += 20;
        else if (cap >= 100)  score += 15;
        else if (cap >= 10)   score += 10;
        else                   score += 5;

        // 成立年限
        int years = ind.getEstablishYears();
        if (years >= 10)      score += 30;
        else if (years >= 5)  score += 25;
        else if (years >= 3)  score += 20;
        else if (years >= 1)  score += 12;
        else                   score += 5;

        // 外贸备案
        if (ind.isHasForeignTradeRecord()) score += 25;
        // 跨境电商备案
        if (ind.isHasCrossBorderEcommerceRecord()) score += 20;

        return Math.min(score, 100);
    }

    // ============ 维度二：经营合规（起始100，扣分制） ============
    private double calculateComplianceScore(RiskIndicators ind) {
        double score = 100;
        score -= Math.min(ind.getAbnormalOperationCount() * 20, 60);
        score -= Math.min(ind.getAdministrativePenaltyCount() * 25, 75);
        if (ind.isInDishonestyList()) score -= 80;
        score -= ind.getRecentAbnormalCount() * 10;
        return Math.max(0, score);
    }

    // ============ 维度三：口岸通关（起始100，扣分制） ============
    private double calculateCustomsScore(RiskIndicators ind) {
        double score = 100;
        int v = ind.getCustomsViolationCount();
        if (v >= 5)       score -= 70;
        else if (v >= 3)  score -= 50;
        else if (v >= 1)  score -= 25;

        double rate = ind.getInspectionAbnormalRate();
        if (rate >= 0.20)  score -= 30;
        else if (rate >= 0.10) score -= 15;
        else if (rate >= 0.05) score -= 5;

        double dev = ind.getCustomsTimeDeviation();
        if (dev >= 3.0)   score -= 20;
        else if (dev >= 2.0) score -= 10;

        return Math.max(0, score);
    }

    // ============ 维度四：贸易活跃（满分100） ============
    private double calculateTradeScore(RiskIndicators ind) {
        double score = 0;
        int cnt = ind.getRecent12MonthDeclarationCount();
        if (cnt >= 24)     score += 40;
        else if (cnt >= 12) score += 30;
        else if (cnt >= 6)  score += 20;
        else if (cnt >= 1)  score += 10;

        int cat = ind.getTradeCategoryCount();
        if (cat >= 1 && cat <= 5) score += 30;
        else if (cat >= 6)         score += 15;

        int cont = ind.getContinuousActiveMonths();
        if (cont >= 12)  score += 30;
        else if (cont >= 6) score += 20;
        else if (cont >= 3) score += 10;

        return Math.min(score, 100);
    }

    // ============ 维度五：综合调整（基准80） ============
    private double calculateAdjustScore(RiskIndicators ind) {
        double score = 80;
        String indCat = ind.getIndustryCategory();
        if (isHighRiskIndustry(indCat))   score -= 20;
        else if (isMediumRiskIndustry(indCat)) score -= 10;
        return Math.max(0, score);
    }

    // ============ 风险标签生成 ============
    private List<String> generateRiskTags(RiskIndicators ind) {
        List<String> tags = new ArrayList<>();
        if (ind.getAbnormalOperationCount() > 0)
            tags.add("存在" + ind.getAbnormalOperationCount() + "条经营异常记录（演示口径，拟接入广西市场监管公开数据）");
        if (ind.getAdministrativePenaltyCount() > 0)
            tags.add("存在" + ind.getAdministrativePenaltyCount() + "条行政处罚记录（演示口径，拟接入广西市场监管公开数据）");
        if (ind.isInDishonestyList())
            tags.add("样例字段命中严重违法失信规则（拟接入广西市场监管公开数据核验）");
        if (ind.getCustomsViolationCount() > 0)
            tags.add("存在" + ind.getCustomsViolationCount() + "次违规报关样例记录（拟接入海关公开信息核验）");
        if (ind.getInspectionAbnormalRate() >= 0.10)
            tags.add(String.format("口岸查验异常率偏高：%.0f%%（演示口径，拟接入口岸公开统计）", ind.getInspectionAbnormalRate() * 100));
        if (ind.getEstablishYears() < 1)
            tags.add("企业成立不足1年，历史信用数据有限（样例字段待权威数据复核）");
        if (!ind.isHasForeignTradeRecord())
            tags.add("样例数据中暂无对外贸易经营者备案记录（拟接入商务公开数据核验）");
        if (ind.getRecentAbnormalCount() > 0)
            tags.add("近1年内新增" + ind.getRecentAbnormalCount() + "条异常记录，需重点关注");
        return tags;
    }

    private boolean isHighRiskIndustry(String industry) {
        String[] high = {"废旧物资", "化工", "矿产品", "木材加工", "冻品"};
        for (String h : high) if (industry != null && industry.contains(h)) return true;
        return false;
    }

    private boolean isMediumRiskIndustry(String industry) {
        String[] med = {"农产品", "水产品", "纺织品", "日用品"};
        for (String m : med) if (industry != null && industry.contains(m)) return true;
        return false;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
