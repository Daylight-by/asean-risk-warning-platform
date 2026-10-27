package com.risk.service;
import com.risk.mapper.CustomsDeclarationMapper;
import com.risk.mapper.SettlementCreditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class FraudPredictionService {
    @Autowired private CustomsDeclarationMapper customsMapper;
    @Autowired private SettlementCreditMapper settlementMapper;
    
    public Map<String, Object> predictFraudRisk(String creditCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 货值波动分析
        double valueFluctuation = analyzeValueFluctuation(creditCode);
        result.put("valueFluctuation", Math.round(valueFluctuation * 100.0) / 100.0);
        result.put("valueFluctuationRisk", valueFluctuation > 50 ? "高" : valueFluctuation > 20 ? "中" : "低");
        // 合作国变更检测
        int countryChanges = detectCountryChanges(creditCode);
        result.put("countryChanges", countryChanges);
        result.put("countryChangeRisk", countryChanges >= 3 ? "高" : countryChanges >= 1 ? "中" : "低");
        // 结算流水异常
        double settlementAnomaly = analyzeSettlementAnomaly(creditCode);
        result.put("settlementAnomaly", Math.round(settlementAnomaly * 100.0) / 100.0);
        result.put("settlementAnomalyRisk", settlementAnomaly > 40 ? "高" : settlementAnomaly > 15 ? "中" : "低");
        // 综合欺诈风险
        double totalRisk = valueFluctuation * 0.4 + countryChanges * 10 * 0.3 + settlementAnomaly * 0.3;
        totalRisk = Math.min(100, Math.max(0, totalRisk));
        result.put("totalFraudRisk", Math.round(totalRisk * 100.0) / 100.0);
        result.put("fraudRiskLevel", totalRisk > 60 ? "高风险" : totalRisk > 30 ? "中风险" : "低风险");

        // 模型说明随接口返回，答辩时可直接说明每个分数的由来与数据边界。
        Map<String, Object> modelProfile = new LinkedHashMap<>();
        modelProfile.put("modelVersion", "cross-border-fraud-scorecard-v1");
        modelProfile.put("predictionPeriod", "未来3个月");
        modelProfile.put("weights", Map.of(
                "declaredValueFluctuation", 0.40,
                "counterpartyCountryDiversity", 0.30,
                "settlementAmountAnomaly", 0.30));
        modelProfile.put("featureSources", List.of(
                "t_customs_declaration.declared_value（项目演示样例/待接入口岸公开数据）",
                "t_settlement_credit.target_country（项目演示样例）",
                "t_settlement_credit.settlement_amount（项目演示样例）"));
        modelProfile.put("dataStatus", "DEMO");
        modelProfile.put("useLimitation", "用于风险提示与比赛演示，不作为执法、授信或退税认定的唯一依据。");
        result.put("modelProfile", modelProfile);
        // 预测预警类型
        List<String> warningTypes = new ArrayList<>();
        if (valueFluctuation > 50) warningTypes.add("虚假出口退税风险");
        if (valueFluctuation > 30) warningTypes.add("低值高报/高值低报风险");
        if (settlementAnomaly > 40) warningTypes.add("跨境资金异常流转风险");
        if (countryChanges >= 3) warningTypes.add("频繁变更贸易伙伴风险");
        result.put("warningTypes", warningTypes);
        result.put("predictionPeriod", "未来3个月");
        return result;
    }
    
    private double analyzeValueFluctuation(String creditCode) {
        try {
            List<Map<String,Object>> monthly = customsMapper.monthlyDeclarationValue(creditCode);
            if (monthly == null || monthly.size() < 3) return 10.0;
            double[] values = new double[monthly.size()];
            for (int i = 0; i < monthly.size(); i++) {
                Object v = monthly.get(i).get("totalValue");
                values[i] = v instanceof Number ? ((Number) v).doubleValue() : 0;
            }
            double avg = 0;
            for (double v : values) avg += v;
            avg /= values.length;
            if (avg == 0) return 10.0;
            double maxDev = 0;
            for (double v : values) {
                double dev = Math.abs(v - avg) / avg * 100;
                if (dev > maxDev) maxDev = dev;
            }
            return maxDev;
        } catch (Exception e) { return 10.0; }
    }
    
    private int detectCountryChanges(String creditCode) {
        try {
            List<Map<String,Object>> records = settlementMapper.uniqueCounterCountries(creditCode);
            return records != null ? records.size() : 0;
        } catch (Exception e) { return 0; }
    }
    
    private double analyzeSettlementAnomaly(String creditCode) {
        try {
            List<Map<String,Object>> records = settlementMapper.recentSettlements(creditCode, 6);
            if (records == null || records.size() < 2) return 10.0;
            double[] amounts = new double[records.size()];
            for (int i = 0; i < records.size(); i++) {
                Object v = records.get(i).get("amount");
                amounts[i] = v != null ? ((Number)v).doubleValue() : 0;
            }
            double avg = 0;
            for (double a : amounts) avg += a;
            avg /= amounts.length;
            if (avg == 0) return 10.0;
            double variance = 0;
            for (double a : amounts) variance += Math.pow(a - avg, 2);
            variance /= amounts.length;
            double cv = Math.sqrt(variance) / avg * 100;
            return Math.min(100, cv);
        } catch (Exception e) { return 10.0; }
    }
}
