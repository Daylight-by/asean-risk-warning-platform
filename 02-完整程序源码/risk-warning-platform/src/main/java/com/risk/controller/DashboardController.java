package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.PortGeo;
import com.risk.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private EnterpriseMapper enterpriseMapper;
    @Autowired private WarningMessageMapper warningMessageMapper;
    @Autowired private PortGeoMapper portGeoMapper;
    @Autowired private CustomsDeclarationMapper customsMapper;
    @Autowired private PolicyMapper policyMapper;

    /**
     * 数据概览 GET /api/dashboard/overview
     * 【面试】同时兼容web-admin字段名(serviceCount/monitorCount)和小程序字段名(enterpriseTotal/warningTotal)
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String,Object>> overview() {
        int total = enterpriseMapper.countAll();
        int warnings = warningMessageMapper.countAll();
        int thisMonthWarnings = warningMessageMapper.countThisMonth();
        int policyTotal = policyMapper.countSearch("", "");

        // 动态计算高风险企业数
        List<Map<String,Object>> allWithScore = enterpriseMapper.findAllWithRiskScore();
        if (allWithScore == null) allWithScore = new ArrayList<>();
        long highRiskCount = allWithScore.stream()
            .filter(m -> m.get("riskScore") != null)
            .filter(m -> ((Number) m.get("riskScore")).doubleValue() <= 40)
            .count();

        // 覆盖国家数（口岸 border_country + 政策 target_country 联合去重，过滤泛称）
        Set<String> countries = new TreeSet<>();
        // 1) 口岸国家
        List<PortGeo> ports = portGeoMapper.findAll();
        if (ports != null) {
            ports.stream()
                .map(PortGeo::getBorderCountry)
                .filter(c -> c != null && !c.isEmpty())
                .forEach(countries::add);
        }
        // 2) 政策目标国家/地区
        List<String> policyCountries = policyMapper.findDistinctCountries();
        if (policyCountries != null) {
            policyCountries.stream()
                .filter(c -> c != null && !c.isEmpty())
                .forEach(countries::add);
        }
        // 过滤掉泛称/聚合标签，只保留真实国家名
        Set<String> genericTerms = new HashSet<>(Arrays.asList("东盟", "东盟多国", "全区", "RCEP成员国", "其他"));
        genericTerms.forEach(countries::remove);
        int countryCount = countries.size();

        String highRiskRatio = total > 0 ? String.format("%.1f%%", highRiskCount * 100.0 / total) : "0%";

        Map<String,Object> data = new LinkedHashMap<>();
        // API.md要求的字段（小程序用）
        data.put("enterpriseTotal", total);
        data.put("warningTotal", warnings);
        data.put("countryCount", (int) countryCount);
        data.put("policyTotal", policyTotal);
        data.put("warningCount", thisMonthWarnings);   // 本月预警=未读
        data.put("unreadCount", thisMonthWarnings);
        data.put("newWarningCount", thisMonthWarnings);
        // web-admin兼容字段
        data.put("serviceCount", total);
        data.put("monitorCount", total);
        data.put("highRiskRatio", highRiskRatio);
        data.put("highRiskCount", (int) highRiskCount);
        return ApiResponse.success(data);
    }

    /** 风险等级分布 GET /api/dashboard/risk-distribution */
    @GetMapping("/risk-distribution")
    public ApiResponse<Map<String,Object>> riskDistribution() {
        List<Map<String,Object>> allWithScore = enterpriseMapper.findAllWithRiskScore();
        if (allWithScore == null) allWithScore = new ArrayList<>();
        int high = 0, medium = 0, low = 0;
        for (Map<String,Object> m : allWithScore) {
            Object scoreObj = m.get("riskScore");
            if (scoreObj == null) { low++; continue; }
            double score = ((Number) scoreObj).doubleValue();
            if (score <= 40) high++;
            else if (score <= 70) medium++;
            else low++;
        }
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("high", high);
        data.put("medium", medium);
        data.put("low", low);
        return ApiResponse.success(data);
    }

    /** 口岸活跃度 GET /api/dashboard/port-activity */
    @GetMapping("/port-activity")
    public ApiResponse<List<Map<String,Object>>> portActivity() {
        List<PortGeo> ports = portGeoMapper.findAll();
        if (ports == null) ports = new ArrayList<>();
        List<Map<String,Object>> list = new ArrayList<>();
        for (PortGeo port : ports) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("portName", port.getPortName());
            m.put("count", customsMapper.countAllByPort(port.getPortName()));
            list.add(m);
        }
        return ApiResponse.success(list);
    }

    /** 月度趋势 GET /api/dashboard/monthly-trend */
    @GetMapping("/monthly-trend")
    public ApiResponse<List<Map<String,Object>>> monthlyTrend() {
        List<Map<String,Object>> dbData = warningMessageMapper.monthlyTrend();
        List<Map<String,Object>> list = new ArrayList<>();
        if (dbData != null && !dbData.isEmpty()) {
            for (Map<String,Object> row : dbData) {
                Map<String,Object> item = new LinkedHashMap<>();
                item.put("month", row.get("month"));
                item.put("count", ((Number) row.get("cnt")).intValue());
                list.add(item);
            }
        }
        return ApiResponse.success(list);
    }

    /** 行业排名 GET /api/dashboard/industry-ranking */
    @GetMapping("/industry-ranking")
    public ApiResponse<List<Map<String,Object>>> industryRanking() {
        List<Map<String,Object>> dbData = enterpriseMapper.groupByIndustry();
        List<Map<String,Object>> list = new ArrayList<>();
        if (dbData != null) {
            for (Map<String,Object> row : dbData) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("industry", row.get("industry_category"));
                m.put("count", ((Number) row.get("cnt")).intValue());
                list.add(m);
            }
        }
        return ApiResponse.success(list);
    }

    /** 口岸风险分布 GET /api/dashboard/port-distribution */
    @GetMapping("/port-distribution")
    public ApiResponse<List<Map<String,Object>>> portDistribution() {
        List<PortGeo> ports = portGeoMapper.findAll();
        if (ports == null) ports = new ArrayList<>();
        List<Map<String,Object>> list = new ArrayList<>();
        for (PortGeo port : ports) {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("portName", port.getPortName());
            item.put("longitude", port.getLongitude());
            item.put("latitude", port.getLatitude());
            item.put("count", customsMapper.countAllByPort(port.getPortName()));
            list.add(item);
        }
        return ApiResponse.success(list);
    }
}
