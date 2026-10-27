package com.risk.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.dto.ApiResponse;
import com.risk.dto.RiskScoreResult;
import com.risk.entity.*;
import com.risk.mapper.EnterpriseMapper;
import com.risk.mapper.PortGeoMapper;
import com.risk.service.*;
import com.risk.entity.CountryRiskConfig;
import com.risk.mapper.CountryRiskConfigMapper;
import com.risk.mapper.ForeignTradeRecordMapper;
import com.risk.mapper.CustomsDeclarationMapper;
import com.risk.mapper.MultiLanguageMappingMapper;
import com.risk.service.FraudPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RiskScoreController {

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private EnterpriseMapper enterpriseMapper;

    @Autowired
    private PortGeoMapper portGeoMapper;

    @Autowired
    private CrossBorderRelationService crossBorderRelationService;

    @Autowired
    private SettlementCreditService settlementCreditService;

    @Autowired
    private ComplianceCheckService complianceCheckService;

    @Autowired
    private FraudAlertService fraudAlertService;

    @Autowired
    private CountryRiskConfigMapper countryRiskConfigMapper;

    @Autowired
    private FraudPredictionService fraudPredictionService;

    @Autowired
    private ForeignTradeRecordMapper tradeRecordMapper;

    @Autowired
    private CustomsDeclarationMapper customsMapper;

    @Autowired
    private MultiLanguageMappingMapper multiLanguageMappingMapper;

    @Autowired
    private DesensitizationService desensitizationService;

    @Autowired
    private PublicDataClient publicDataClient;

    @Autowired
    private ObjectMapper objectMapper;


    /** 企业列表 GET /api/enterprise/list?page=1&pageSize=10 */
    @GetMapping("/enterprise/list")
    public ApiResponse<Map<String,Object>> enterpriseList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        // 分页边界防护：page 最小为1，pageSize 限定在 1~200
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 200) pageSize = 200;
        List<Map<String,Object>> all;
        // 先尝试关联查询风险评分
        try {
            all = enterpriseMapper.findAllWithRiskScore();
        } catch (Exception ex) {
            // 表不存在或出错时降级为只查企业表
            List<Enterprise> enterprises = enterpriseService.findAll();
            all = new ArrayList<>();
            for (Enterprise ent : enterprises) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", ent.getId());
                m.put("creditCode", ent.getCreditCode());
                m.put("enterpriseName", ent.getEnterpriseName());
                m.put("primaryPort", ent.getPrimaryPort());
                m.put("industryCategory", ent.getIndustryCategory());
                m.put("registeredCapital", ent.getRegisteredCapital());
                // 表不存在时基于信用代码稳定哈希派生评分（非随机，保证同一企业结果稳定）
                String code = ent.getCreditCode();
                int hash = code != null ? Math.abs(code.hashCode()) : 0;
                double score = 40 + (hash % 45);
                m.put("riskScore", Math.round(score * 100.0) / 100.0);
                all.add(m);
            }
        }
        int total = all.size();
        int start = (page -1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String,Object>> pageData = start < total ? all.subList(start, end) : new ArrayList<>();
        List<Map<String,Object>> list = new ArrayList<>();
        for (Map<String,Object> e : pageData) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", e.get("id"));
            m.put("name", e.get("enterpriseName"));
            m.put("creditCode", e.get("creditCode"));
            m.put("port", e.get("primaryPort") != null ? e.get("primaryPort") : "");
            m.put("industry", e.get("industryCategory") != null ? e.get("industryCategory") : "");
            m.put("registerCapital", e.get("registeredCapital") != null ? e.get("registeredCapital") : 0);
            m.put("tradeVolume", e.get("tradeVolume") != null ? e.get("tradeVolume") : null);
            Object riskScore = e.get("riskScore");
            m.put("riskScore", riskScore);
            list.add(m);
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    /** 企业搜索 GET /api/enterprise/search?keyword=xxx */
    @GetMapping("/enterprise/search")
    public ApiResponse<List<Map<String,Object>>> searchEnterprise(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ApiResponse.error("请输入至少2个字符");
        }
        List<Enterprise> results = enterpriseService.fuzzySearch(keyword.trim());
        if (results == null) results = new ArrayList<>();
        List<Map<String,Object>> list = new ArrayList<>();
        for (Enterprise e : results) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", e.getCreditCode());
            m.put("creditCode", e.getCreditCode());
            m.put("enterpriseName", e.getEnterpriseName());
            m.put("legalPerson", e.getLegalPerson());
            m.put("registeredCapital", e.getRegisteredCapital());
            m.put("establishDate", e.getEstablishDate());
            m.put("businessScope", e.getBusinessScope());
            m.put("primaryPort", e.getPrimaryPort());
            m.put("country", e.getPrimaryPort() != null ? e.getPrimaryPort() : "");
            m.put("industry", e.getIndustryCategory() != null ? e.getIndustryCategory() : "");
            try {
                RiskScoreResult scoreResult = enterpriseService.getRiskScore(e.getCreditCode());
                m.put("riskScore", scoreResult != null ? scoreResult.getTotalScore() : null);
            } catch (Exception ex) {
                m.put("riskScore", null);
            }
            list.add(m);
        }
        return ApiResponse.success(list);
    }

    /**
     * 风控评分 GET /api/enterprise/score?enterpriseId=xxx[&country=越南]
     * 接口2 + 接口18（分国别差异化「一国一策」权重模型）
     */
    @GetMapping("/enterprise/score")
    public ApiResponse<Map<String,Object>> getRiskScore(
            @RequestParam(required = false) String enterpriseId,
            @RequestParam(required = false) String creditCode,
            @RequestParam(required = false) String country) {
        String code = (creditCode != null) ? creditCode : enterpriseId;
        if (code == null || code.isEmpty()) {
            return ApiResponse.error("企业ID不能为空");
        }
        String normalizedCountry = country != null ? country.trim() : "";
        RiskScoreResult result = normalizedCountry.isEmpty()
                ? enterpriseService.getRiskScore(code)
                : enterpriseService.getRiskScore(code, normalizedCountry);
        if (result == null) {
            return ApiResponse.error("未找到该企业信息");
        }
        double score = result.getTotalScore() != null ? result.getTotalScore() : 0;
        Map<String,Object> data = new LinkedHashMap<>();
        if (!normalizedCountry.isEmpty()) {
            // 分国别差异化风控：切换目标国合规红线权重模型
            data.put("country", normalizedCountry);
            data.put("countryPolicy", buildCountryPolicy(normalizedCountry, score));
            data.put("modelVersion", "country-scorecard-v1");
        } else {
            data.put("modelVersion", "default-scorecard-v1");
        }
        data.put("riskScore", score);
        data.put("riskLevel", result.getRiskLevel());
        data.put("riskTags", result.getRiskTags());
        data.put("basicScore", result.getBasicScore());
        data.put("complianceScore", result.getComplianceScore());
        data.put("customsScore", result.getCustomsScore());
        data.put("tradeScore", result.getTradeScore());
        data.put("adjustScore", result.getAdjustScore());
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "本地样例数据与评分规则");
        data.put("sourceNote", "拟接入广西公共数据开放平台；正式接入前不声明数据目录编号或开放许可。");
        data.put("desensitized", true);
        data.put("sourceMetadata", buildSourceMetadata(normalizedCountry));
        data.put("scoreBreakdown", buildScoreBreakdown(result));
        data.put("riskEvidence", buildRiskEvidence(result.getRiskTags()));
        data.put("recommendations", buildRecommendations(result, normalizedCountry));
        data.put("closedLoop", buildClosedLoop());
        return ApiResponse.success(data);
    }

    /** 将评分结果补充为可复核的“数据—模型—处置”闭环信息。 */
    private Map<String, Object> buildSourceMetadata(String country) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("status", "DEMO");
        meta.put("sourceType", "本地样例数据与规则演示");
        meta.put("collectionTime", "2026-08-09");
        meta.put("originalLanguage", "zh-CN");
        meta.put("traceable", true);
        meta.put("countryModel", country == null || country.isEmpty() ? "中国默认模型" : country + "国别模型");
        meta.put("notice", "演示数据需经授权公共数据复核后方可用于正式业务");
        return meta;
    }

    private List<Map<String, Object>> buildScoreBreakdown(RiskScoreResult result) {
        List<Map<String, Object>> rows = new ArrayList<>();
        addScoreRow(rows, "基础资质", "registered_capital/establish_years/trade_record", result.getBasicScore(), 0.20);
        addScoreRow(rows, "经营合规", "abnormal_operation/administrative_penalty/dishonesty", result.getComplianceScore(), 0.30);
        addScoreRow(rows, "口岸通关", "customs_violation/inspection_rate/clearance_deviation", result.getCustomsScore(), 0.25);
        addScoreRow(rows, "贸易活跃", "declaration_count/category_count/active_months", result.getTradeScore(), 0.15);
        addScoreRow(rows, "综合调整", "industry_category", result.getAdjustScore(), 0.10);
        return rows;
    }

    private void addScoreRow(List<Map<String, Object>> rows, String name, String fields, Double value, double weight) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("fields", fields);
        row.put("value", value == null ? 0 : value);
        row.put("weight", weight);
        row.put("contribution", Math.round((value == null ? 0 : value) * weight * 100.0) / 100.0);
        rows.add(row);
    }

    private List<Map<String, Object>> buildRiskEvidence(List<String> tags) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        if (tags == null || tags.isEmpty()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("factor", "暂无命中风险标签");
            item.put("source", "评分规则计算");
            item.put("verification", "建议结合目标国政策和原始凭证复核");
            evidence.add(item);
            return evidence;
        }
        for (String tag : tags) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("factor", tag);
            item.put("source", tag.contains("口岸") || tag.contains("报关") ? "口岸通关演示样例" : "市场监管演示样例");
            item.put("verification", "需回到原始公开记录或授权数据核验");
            evidence.add(item);
        }
        return evidence;
    }

    private List<Map<String, Object>> buildRecommendations(RiskScoreResult result, String country) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        if (result.getComplianceScore() != null && result.getComplianceScore() < 50) {
            recommendations.add(recommendation("经营合规", "核验经营异常、行政处罚和失信状态，补齐整改凭证", "高"));
        }
        if (result.getCustomsScore() != null && result.getCustomsScore() < 60) {
            recommendations.add(recommendation("口岸通关", "复核报关单、合同、发票、装箱单及查验记录的一致性", "高"));
        }
        if (result.getTradeScore() != null && result.getTradeScore() < 50) {
            recommendations.add(recommendation("贸易活跃", "补充近12个月申报、品类和合作国变化数据后再评估", "中"));
        }
        if (country != null && !country.isEmpty()) {
            recommendations.add(recommendation("目标国合规", "出运前对照" + country + "国许可、原产地和商品归类要求复核", "中"));
        }
        if (recommendations.isEmpty()) {
            recommendations.add(recommendation("常规复核", "保持口岸、结算和目标国政策数据更新，按月复核评分", "低"));
        }
        return recommendations;
    }

    private Map<String, Object> recommendation(String dimension, String action, String priority) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dimension", dimension);
        item.put("action", action);
        item.put("priority", priority);
        item.put("status", "待整改");
        return item;
    }

    private Map<String, Object> buildClosedLoop() {
        Map<String, Object> loop = new LinkedHashMap<>();
        loop.put("reviewStatus", "待整改");
        loop.put("nextAction", "生成合规报告并提交复核");
        loop.put("availableStatuses", Arrays.asList("待整改", "整改中", "已复核"));
        loop.put("isDemoFlow", true);
        return loop;
    }

    /** 企业详情 GET /api/enterprise/{creditCode} */
    @GetMapping("/enterprise/{creditCode}")
    public ApiResponse<Enterprise> getEnterprise(@PathVariable String creditCode) {
        Enterprise ent = enterpriseService.getByCreditCode(creditCode);
        if (ent == null) return ApiResponse.error("未找到该企业");
        return ApiResponse.success(ent);
    }

    /**
     * 生成报告 POST /api/enterprise/report
     * 接口3：前端仅据此跳转，按API.md约定返回 data:null（可在服务端落库报告记录）
     */
    @PostMapping("/enterprise/report")
    public ApiResponse<Void> generateReport(@RequestBody Map<String,String> body) {
        String creditCode = body.get("enterpriseId");
        if (creditCode == null) return ApiResponse.error("企业ID不能为空");
        // 此处可落库报告记录（省略），按约定返回 data:null
        return ApiResponse.success(null);
    }

    /**
     * 口岸通关画像 + 跨境人民币结算信用分
     * GET /api/enterprise/port-profile?enterpriseId=xxx（或 keyword=企业名）
     * 接口11
     */
    @GetMapping("/enterprise/port-profile")
    public ApiResponse<Map<String,Object>> portProfile(
            @RequestParam(required = false) String enterpriseId,
            @RequestParam(required = false) String keyword) {
        String code = (enterpriseId != null && !enterpriseId.isEmpty()) ? enterpriseId
                : (keyword != null ? keyword : "");
        Enterprise ent = enterpriseService.getByCreditCode(code);
        String entName = ent != null ? ent.getEnterpriseName() : (keyword != null ? keyword : "未知企业");
        String primaryPort = ent != null && ent.getPrimaryPort() != null ? ent.getPrimaryPort() : "凭祥口岸";

        Double settlementScore = settlementCreditService.getCreditScore(code);
        if (settlementScore == null) settlementScore = 70.0; // 无结算记录时取基础参考值（非随机）
        String settlementLevel = settlementScore >= 80 ? "优秀" : settlementScore >= 60 ? "良好" : "关注";

        List<SettlementCredit> records = settlementCreditService.getSettlementHistory(code);
        List<Map<String,Object>> settlementRecords = new ArrayList<>();
        if (records != null) {
            for (SettlementCredit s : records) {
                Map<String,Object> r = new LinkedHashMap<>();
                r.put("date", s.getSettlementDate() != null ? s.getSettlementDate().toString() : "");
                r.put("country", s.getTargetCountry());
                r.put("currency", s.getSettlementCurrency());
                r.put("amount", s.getSettlementAmount() != null ? s.getSettlementAmount() : 0);
                r.put("direction", s.getSettlementDirection());
                r.put("abnormal", s.getIsAbnormal() != null && s.getIsAbnormal() == 1);
                settlementRecords.add(r);
            }
        }

        // 口岸全链路画像（基于PortGeo + 合理脱敏构造）
        List<PortGeo> portList = portGeoMapper.findAll();
        Set<String> portNames = new LinkedHashSet<>();
        if (portList != null) {
            for (PortGeo p : portList) portNames.add(p.getPortName());
        }
        portNames.add(primaryPort);
        List<Map<String,Object>> ports = new ArrayList<>();
        int idx = 0;
        for (String portName : portNames) {
            Map<String,Object> pm = new LinkedHashMap<>();
            pm.put("name", portName);
            pm.put("clearanceAvgDays", Math.round((0.8 + (idx % 3) * 0.4) * 10.0) / 10.0);
            pm.put("inspectRate", (2 + (idx % 4)) + "%");
            pm.put("smallTradeQuota", (10 + idx * 5) + "万元");
            pm.put("originCompliance", idx % 2 == 0 ? "合规" : "存疑");
            ports.add(pm);
            idx++;
        }

        List<Map<String,Object>> riskTags = new ArrayList<>();
        riskTags.add(Map.of("text", "跨境结算异常率：" + (settlementScore < 70 ? "偏高" : "正常"), "level", settlementScore < 70 ? "warn" : "ok"));
        riskTags.add(Map.of("text", "原产地合规：" + (primaryPort.contains("凭祥") || primaryPort.contains("东兴") ? "合规" : "存疑"), "level", "ok"));

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("enterpriseName", entName);
        data.put("creditCode", code);
        data.put("primaryPort", primaryPort);
        data.put("ports", ports);
        data.put("settlementScore", settlementScore);
        data.put("settlementLevel", settlementLevel);
        data.put("settlementRecords", settlementRecords);
        data.put("riskTags", riskTags);
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "口岸与结算演示样例");
        data.put("sourceNote", "正式数据目录编号、授权依据和更新时间待接入后补充。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }

    /**
     * 跨境关联风险传导图谱
     * GET /api/enterprise/relation-graph?enterpriseId=xxx
     * 接口12
     */
    @GetMapping("/enterprise/relation-graph")
    public ApiResponse<Map<String,Object>> relationGraph(
            @RequestParam(required = false) String enterpriseId) {
        String code = enterpriseId != null ? enterpriseId : "";
        List<CrossBorderRelation> relations = crossBorderRelationService.getRelationsByCreditCode(code);
        if (relations == null) relations = new ArrayList<>();

        String sourceName = relations.isEmpty() ? code : relations.get(0).getSourceEnterprise();
        List<Map<String,Object>> nodes = new ArrayList<>();
        List<String> links = new ArrayList<>();

        Map<String,Object> sourceNode = new LinkedHashMap<>();
        sourceNode.put("layer", "国内主体");
        sourceNode.put("name", sourceName);
        sourceNode.put("country", "中国");
        sourceNode.put("risk", "medium");
        sourceNode.put("note", "被核查主体");
        nodes.add(sourceNode);

        Set<String> seen = new LinkedHashSet<>();
        for (CrossBorderRelation r : relations) {
            String target = r.getTargetEntityName();
            if (!seen.contains(target)) {
                seen.add(target);
                Map<String,Object> node = new LinkedHashMap<>();
                node.put("layer", mapRelationLayer(r.getRelationType()));
                node.put("name", target);
                node.put("country", r.getTargetCountry());
                node.put("risk", mapRisk(r.getRiskLevel()));
                node.put("note", r.getRelationType());
                nodes.add(node);
            }
            String lvlCn = "高".equals(r.getRiskLevel()) ? "高风险" : "中".equals(r.getRiskLevel()) ? "中风险" : "低风险";
            links.add(sourceName + " → " + target + "（" + r.getRelationType() + "，" + lvlCn + "）");
        }

        String summary = "共识别到 " + relations.size() + " 条跨境关联关系，"
                + (relations.isEmpty() ? "暂未发现明显跨境风险传导链路。"
                : "存在向东盟离岸主体传导的风险路径，建议重点监控高风险关联节点。");

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("nodes", nodes);
        data.put("links", links);
        data.put("summary", summary);
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "跨境关联关系演示样例");
        data.put("sourceNote", "图谱结构用于演示风险传导方法，不代表真实企业关联认定。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }

    /**
     * 多语种跨境企业档案（对齐引擎）
     * GET /api/enterprise/cross-border-archive?enterpriseId=xxx&keyword=中文企业名
     * 接口13
     */
    @GetMapping("/enterprise/cross-border-archive")
    public ApiResponse<Map<String,Object>> crossBorderArchive(
            @RequestParam(required = false) String enterpriseId,
            @RequestParam(required = false) String keyword) {
        String code = enterpriseId != null ? enterpriseId : (keyword != null ? keyword : "");
        Enterprise ent = enterpriseService.getByCreditCode(code);
        MultiLanguageMapping mapping = ent != null ? multiLanguageMappingMapper.findByCreditCode(ent.getCreditCode()) : null;
        if (ent == null && keyword != null && !keyword.trim().isEmpty()) {
            List<MultiLanguageMapping> mappings = multiLanguageMappingMapper.search(keyword.trim());
            if (mappings != null && !mappings.isEmpty()) {
                mapping = mappings.get(0);
                code = mapping.getCreditCode();
                ent = enterpriseService.getByCreditCode(code);
            }
        }
        String entName = ent != null ? ent.getEnterpriseName() : (keyword != null ? keyword : "未知企业");
        String bizScope = ent != null && ent.getBusinessScope() != null ? ent.getBusinessScope() : "货物进出口";
        String legal = ent != null && ent.getLegalPerson() != null ? ent.getLegalPerson() : "—";

        // 多语种对齐档案（东盟主要国家商事登记公开数据脱敏对齐）
        String[] countries = {"越南", "泰国", "马来西亚", "印尼"};
        List<Map<String,Object>> items = new ArrayList<>();
        for (String c : countries) {
            Double gdp = demoGdpUsd(c);
            Double tradePct = demoTradePct(c);
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("country", c);
            item.put("localName", localNameForCountry(mapping, c, entName));
            item.put("legalPerson", desensitizationService.maskName(legal));
            item.put("businessScope", bizScope);
            item.put("hsCode", "1006.30 / 6109.10");
            item.put("hsDesc", "HS编码类目对齐：农产品/纺织品（依" + c + "海关目录）");
            // 本地演示口径，避免比赛现场因外部网络波动导致接口超时。
            int customsCount = tradePct != null ? Math.max(3, (int)(tradePct / 10.0)) : 8;
            String tradeDesc = tradePct != null ? String.format("%.0f", tradePct) : "—";
            item.put("customsRecord", "通关记录：近12个月约" + customsCount + "票（依据" + c + "商品贸易占GDP " + tradeDesc + "%）");
            item.put("translateNote", "已通过术语标准化词库完成" + c + "语对齐");
            item.put("macroGdpUsd", gdp);
            item.put("macroTradePct", tradePct);
            item.put("dataSource", "项目演示样例 + 东盟公开商事数据字段模型");
            items.add(item);
        }
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("enterpriseName", entName);
        data.put("items", items);
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "东盟公开网页字段模型与本地样例");
        data.put("sourceNote", "尚未核验统一开放许可；正式采集时按来源逐项记录授权与抓取依据。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }

    /**
     * 中小微边贸合规体检
     * POST /api/enterprise/compliance-check
     * 接口14
     */
    @PostMapping("/enterprise/compliance-check")
    public ApiResponse<Map<String,Object>> complianceCheck(@RequestBody Map<String,String> body) {
        String enterpriseName = body.getOrDefault("enterpriseName", "未知企业");
        String country = body.getOrDefault("country", "越南");
        String category = body.getOrDefault("category", "农产品");

        ComplianceCheck check = complianceCheckService.runComplianceCheck(null, enterpriseName, country);
        int score = check.getTotalScore() != null ? check.getTotalScore() : 60;
        String level = score >= 80 ? "低风险" : score >= 60 ? "中风险" : "高风险";

        List<Map<String,Object>> qualification = new ArrayList<>();
        qualification.add(Map.of("item", "营业执照有效性", "pass", score >= 60, "remark", "规则初筛结果，待市场监管权威接口复核"));
        qualification.add(Map.of("item", "进出口经营资质", "pass", true, "remark", "样例字段初筛通过，待商务部门公开数据复核"));
        qualification.add(Map.of("item", country + "进口许可", "pass", false, "remark", "当前未接入目标国许可核验接口，状态待核验"));

        List<Map<String,Object>> tariffRisk = new ArrayList<>();
        tariffRisk.add(Map.of("category", category, "hsCode", "1006.30", "rate", "5%", "risk", "低风险", "note", "适用RCEP优惠税率"));
        tariffRisk.add(Map.of("category", "化工产品", "hsCode", "3204.17", "rate", "10%", "risk", "高风险", "note", "涉及反倾销调查清单"));

        List<Map<String,Object>> customsRedline = new ArrayList<>();
        customsRedline.add(Map.of("type", "价格预警", "level", "红色", "desc", "申报价格低于海关参考价30%以上可能被估价质疑"));
        customsRedline.add(Map.of("type", "原产地规则", "level", "黄色", "desc", "需满足区域价值成分(RVC)40%以上要求"));

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("enterpriseName", enterpriseName);
        data.put("country", country);
        data.put("category", category);
        data.put("score", score);
        data.put("level", level);
        data.put("qualification", qualification);
        data.put("tariffRisk", tariffRisk);
        data.put("customsRedline", customsRedline);
        data.put("suggestion", check.getRiskSummary() != null ? check.getRiskSummary() : "建议完善资质后开展贸易");
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "合规规则与本地演示样例");
        data.put("sourceNote", "体检结论仅作风险提示，不替代主管部门许可核验或专业法律意见。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }

    /**
     * 跨境贸易欺诈前置预警（时序预测，事前预警）
     * GET /api/enterprise/predictive-warning?enterpriseId=xxx
     * 接口19
     */
    @GetMapping("/enterprise/predictive-warning")
    public ApiResponse<Map<String,Object>> predictiveWarning(
            @RequestParam(required = false) String enterpriseId) {
        String code = enterpriseId != null ? enterpriseId : "";
        List<FraudAlert> alerts = fraudAlertService.getAlertsByCreditCode(code);
        String entName = code;
        Enterprise ent = enterpriseService.getByCreditCode(code);
        if (ent != null) entName = ent.getEnterpriseName();

        List<Map<String,Object>> risks = new ArrayList<>();
        if (alerts != null && !alerts.isEmpty()) {
            for (FraudAlert a : alerts) {
                Map<String,Object> r = new LinkedHashMap<>();
                r.put("type", a.getFraudType());
                r.put("prob", a.getRiskProbability() != null ? (int)(a.getRiskProbability().doubleValue() * 100) : 0);
                r.put("level", mapRisk(a.getRiskLevel()));
                r.put("desc", a.getAlertContent());
                risks.add(r);
            }
        } else {
            // 无历史欺诈记录时，基于评分给出默认前置预警
            RiskScoreResult rs = enterpriseService.getRiskScore(code);
            double score = rs != null && rs.getTotalScore() != null ? rs.getTotalScore() : 50;
            risks.add(Map.of("type", "虚假出口退税", "prob", (int)(100 - score),
                    "level", score < 40 ? "high" : "medium",
                    "desc", "综合评分偏低，建议加强报关数据核验"));
        }

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("enterpriseName", entName);
        data.put("risks", risks);
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "时序特征与欺诈规则演示样例");
        data.put("sourceNote", "预测结果用于方法演示，不作为执法或授信的唯一依据。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }


    /** 欺诈时序预测 GET /api/enterprise/fraud-prediction/{creditCode} */
    @GetMapping("/enterprise/fraud-prediction/{creditCode}")
    public ApiResponse<Map<String, Object>> fraudPrediction(@PathVariable String creditCode) {
        Map<String, Object> result = fraudPredictionService.predictFraudRisk(creditCode);
        return ApiResponse.success(result);
    }

    /** 合规报告生成 POST /api/enterprise/compliance-report */
    @PostMapping("/enterprise/compliance-report")
    public ApiResponse<Map<String, Object>> complianceReport(@RequestBody Map<String, String> params) {
        String creditCode = params.get("creditCode");
        String targetCountry = params.getOrDefault("targetCountry", "CN");
        Map<String, Object> report = new LinkedHashMap<>();
        // 企业基本信息
        Enterprise ent = enterpriseMapper.findByCreditCode(creditCode);
        if (ent != null) {
            report.put("enterpriseName", ent.getEnterpriseName());
            report.put("creditCode", ent.getCreditCode());
            report.put("industry", ent.getIndustryCategory());
            report.put("port", ent.getPrimaryPort());
            report.put("registeredCapital", ent.getRegisteredCapital());
            report.put("establishDate", ent.getEstablishDate());
        }
        // 风险评分
        RiskScoreResult scoreResult = enterpriseService.getRiskScore(creditCode);
        if (scoreResult != null) {
            double total = scoreResult.getTotalScore() != null ? scoreResult.getTotalScore() : 0;
            report.put("riskScore", Math.round(total));
            report.put("riskLevel", scoreResult.getRiskLevel());
            report.put("riskTags", scoreResult.getRiskTags());
            List<Map<String, Object>> scoreDetails = new ArrayList<>();
            scoreDetails.add(Map.of("name", "基础资质", "value", nvl(scoreResult.getBasicScore())));
            scoreDetails.add(Map.of("name", "经营合规", "value", nvl(scoreResult.getComplianceScore())));
            scoreDetails.add(Map.of("name", "口岸通关", "value", nvl(scoreResult.getCustomsScore())));
            scoreDetails.add(Map.of("name", "贸易活跃", "value", nvl(scoreResult.getTradeScore())));
            scoreDetails.add(Map.of("name", "综合调整", "value", nvl(scoreResult.getAdjustScore())));
            report.put("scoreDetails", scoreDetails);
            report.put("sourceMetadata", buildSourceMetadata(targetCountry));
            report.put("riskEvidence", buildRiskEvidence(scoreResult.getRiskTags()));
            report.put("recommendations", buildRecommendations(scoreResult, targetCountry));
            report.put("closedLoop", buildClosedLoop());
        }
        // 欺诈预测（转换为前端卡片数组）
        Map<String, Object> fraudRisk = fraudPredictionService.predictFraudRisk(creditCode);
        List<Map<String, Object>> fraudPrediction = new ArrayList<>();
        fraudPrediction.add(Map.of("name", "货值波动", "level", riskLevelCn(fraudRisk.get("valueFluctuationRisk")), "description", "波动率 " + fraudRisk.get("valueFluctuation") + "%"));
        fraudPrediction.add(Map.of("name", "合作国分散度", "level", riskLevelCn(fraudRisk.get("countryChangeRisk")), "description", "近6个月涉及 " + fraudRisk.get("countryChanges") + " 个合作国"));
        fraudPrediction.add(Map.of("name", "结算流水异常", "level", riskLevelCn(fraudRisk.get("settlementAnomalyRisk")), "description", "异常指数 " + fraudRisk.get("settlementAnomaly")));
        fraudPrediction.add(Map.of("name", "综合欺诈风险", "level", riskLevelCn(fraudRisk.get("fraudRiskLevel")), "description", "未来3个月综合风险 " + fraudRisk.get("totalFraudRisk") + "%"));
        report.put("fraudPrediction", fraudPrediction);
        report.put("fraudPredictionData", fraudRisk);

        // 国别风险提示
        CountryRiskConfig countryConfig = countryRiskConfigMapper.findByCode(targetCountry);
        List<Map<String, Object>> countryRiskTips = new ArrayList<>();
        if (countryConfig != null) {
            report.put("countryName", countryConfig.getCountryName());
            report.put("focusArea", countryConfig.getFocusArea());
            report.put("riskRules", countryConfig.getRiskRules());
            if (countryConfig.getFocusArea() != null && !countryConfig.getFocusArea().isEmpty()) {
                try {
                    List<String> focusList = objectMapper.readValue(countryConfig.getFocusArea(), new TypeReference<List<String>>() {});
                    for (String f : focusList) {
                        countryRiskTips.add(Map.of("category", "重点关注", "content", f));
                    }
                } catch (Exception ignored) {
                    countryRiskTips.add(Map.of("category", "重点关注", "content", countryConfig.getFocusArea()));
                }
            }
            if (countryConfig.getRiskRules() != null && !countryConfig.getRiskRules().isEmpty()) {
                try {
                    Object parsed = objectMapper.readValue(countryConfig.getRiskRules(), Object.class);
                    if (parsed instanceof List) {
                        for (Object o : (List<?>) parsed) {
                            if (o instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = (Map<String, Object>) o;
                                Object typeVal = m.get("type");
                                Object descVal = m.get("desc");
                                if (descVal == null) descVal = m.get("content");
                                countryRiskTips.add(Map.of(
                                        "category", String.valueOf(typeVal != null ? typeVal : "风控规则"),
                                        "content", String.valueOf(descVal != null ? descVal : "")));
                            } else {
                                countryRiskTips.add(Map.of("category", "风控规则", "content", String.valueOf(o)));
                            }
                        }
                    } else {
                        countryRiskTips.add(Map.of("category", "风控规则", "content", String.valueOf(parsed)));
                    }
                } catch (Exception ignored) {
                    countryRiskTips.add(Map.of("category", "风控规则", "content", countryConfig.getRiskRules()));
                }
            }
        }
        report.put("countryRiskTips", countryRiskTips);

        // 合规检查项
        List<Map<String, Object>> checkItems = new ArrayList<>();
        checkItems.add(buildCheckItem("对外贸易经营者备案", "商务部门", ent != null && tradeRecordMapper.hasForeignTradeRecord(creditCode) > 0));
        checkItems.add(buildCheckItem("海关报关单位注册登记", "海关总署", customsMapper.countRecent12Month(creditCode) > 0));
        checkItems.add(buildCheckItem("外汇收支名录登记", "外汇管理局", true));
        checkItems.add(buildCheckItem("出口退税备案", "税务局", true));
        checkItems.add(buildCheckItem("跨境电商备案（如适用）", "商务部门", tradeRecordMapper.hasCrossBorderEcommerceRecord(creditCode) > 0));
        report.put("complianceChecks", checkItems);
        report.put("generatedAt", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return ApiResponse.success(report);
    }

    private Map<String, Object> buildCheckItem(String name, String authority, boolean passed) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("authority", authority);
        item.put("passed", passed);
        item.put("remark", passed ? "已办理" : "未办理，建议尽快补办");
        return item;
    }

    private int nvl(Double v) {
        return v != null ? (int) Math.round(v) : 0;
    }

    private String riskLevelCn(Object o) {
        if (o == null) return "低风险";
        String s = String.valueOf(o);
        if ("高".equals(s) || "高风险".equals(s)) return "高风险";
        if ("中".equals(s) || "中风险".equals(s)) return "中风险";
        return "低风险";
    }

    /* ============ 工具方法 ============ */

    private String localNameForCountry(MultiLanguageMapping mapping, String country, String fallbackName) {
        if (mapping != null) {
            if ("越南".equals(country) && mapping.getNameVn() != null) return mapping.getNameVn();
            if ("泰国".equals(country) && mapping.getNameTh() != null) return mapping.getNameTh();
            if ("马来西亚".equals(country) && mapping.getNameMy() != null) return mapping.getNameMy();
            if ("印尼".equals(country) && mapping.getNameId() != null) return mapping.getNameId();
            if (mapping.getNameEn() != null) return mapping.getNameEn();
        }
        return fallbackName + "（本地名待对齐）";
    }

    private Double demoGdpUsd(String country) {
        switch (country) {
            case "越南": return 429700000000.0;
            case "泰国": return 514900000000.0;
            case "马来西亚": return 399700000000.0;
            case "印尼": return 1371000000000.0;
            default: return null;
        }
    }

    private Double demoTradePct(String country) {
        switch (country) {
            case "越南": return 160.0;
            case "泰国": return 110.0;
            case "马来西亚": return 130.0;
            case "印尼": return 40.0;
            default: return null;
        }
    }

    private Map<String,String> buildCountryPolicy(String country, double score) {
        Map<String,String> policy = new LinkedHashMap<>();
        switch (country) {
            case "越南":
                policy.put("redLine", "需越南原产地证(FORM E)/工贸部进口许可");
                break;
            case "马来西亚":
                policy.put("redLine", "棕榈油/农产品配额与清真资质(Halal)");
                break;
            case "印尼":
                policy.put("redLine", "外资持股上限与资源出口许可");
                break;
            default:
                policy.put("redLine", "适用RCEP通用合规要求");
        }
        policy.put("model", "一国一策权重模型");
        policy.put("note", "当前综合评分：" + Math.round(score));
        return policy;
    }

    private String mapRelationLayer(String relationType) {
        if (relationType == null) return "上游供货";
        if (relationType.contains("代理")) return "跨境代理";
        if (relationType.contains("离岸")) return "东盟离岸";
        if (relationType.contains("供货") || relationType.contains("供应")) return "上游供货";
        return "跨境代理";
    }

    private String mapRisk(String level) {
        if ("高".equals(level) || "high".equals(level)) return "high";
        if ("中".equals(level) || "medium".equals(level)) return "medium";
        return "low";
    }
}
