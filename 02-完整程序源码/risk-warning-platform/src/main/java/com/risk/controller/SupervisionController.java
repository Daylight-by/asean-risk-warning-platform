package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.PortGeo;
import com.risk.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/supervision")
@CrossOrigin
public class SupervisionController {

    @Autowired private EnterpriseMapper enterpriseMapper;
    @Autowired private WarningMessageMapper warningMessageMapper;
    @Autowired private PortGeoMapper portGeoMapper;
    @Autowired private CustomsDeclarationMapper customsMapper;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        int totalEnterprises = enterpriseMapper.countAll();
        int todayAlerts = warningMessageMapper.countThisMonth();

        // 从真实评分数据计算高风险企业
        List<Map<String, Object>> allWithScore = enterpriseMapper.findAllWithRiskScore();
        if (allWithScore == null) allWithScore = new ArrayList<>();
        List<Map<String, Object>> highRiskEnterprises = new ArrayList<>();
        int highRiskCount = 0;
        for (Map<String, Object> m : allWithScore) {
            Object scoreObj = m.get("riskScore");
            if (scoreObj != null && ((Number) scoreObj).doubleValue() <= 40) {
                highRiskCount++;
                Map<String, Object> ent = new LinkedHashMap<>();
                ent.put("name", m.get("enterpriseName"));
                ent.put("port", m.get("primaryPort"));
                ent.put("riskScore", ((Number) scoreObj).doubleValue());
                ent.put("riskType", "综合风险");
                ent.put("relatedCount", 0);
                highRiskEnterprises.add(ent);
            }
        }
        // 按评分升序（风险最高在前），取前7条
        highRiskEnterprises.sort(Comparator.comparingDouble(m -> ((Number) m.get("riskScore")).doubleValue()));
        List<Map<String, Object>> topHighRisk = highRiskEnterprises.size() > 7
            ? highRiskEnterprises.subList(0, 7) : highRiskEnterprises;

        // 口岸数据
        List<PortGeo> ports = portGeoMapper.findAll();
        List<Map<String, Object>> portList = new ArrayList<>();
        for (PortGeo p : ports) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("portName", p.getPortName());
            m.put("longitude", p.getLongitude());
            m.put("latitude", p.getLatitude());
            int count = customsMapper.countAllByPort(p.getPortName());
            m.put("count", count);
            m.put("riskScore", Math.min(95, 30 + count * 5));
            portList.add(m);
        }

        // 真实预警消息
        List<Map<String, Object>> alerts = new ArrayList<>();
        List<com.risk.entity.WarningMessage> latestWarnings = warningMessageMapper.findLatest(10);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (com.risk.entity.WarningMessage w : latestWarnings) {
            Map<String, Object> a = new LinkedHashMap<>();
            String time = w.getCreateTime() != null ? sdf.format(w.getCreateTime()) : "--:--:--";
            String level = "高风险".equals(w.getWarningLevel()) || "high".equals(w.getWarningLevel())
                ? "high" : "中风险".equals(w.getWarningLevel()) || "medium".equals(w.getWarningLevel())
                ? "medium" : "low";
            a.put("time", time);
            a.put("level", level);
            a.put("levelLabel", "high".equals(level) ? "红色" : "medium".equals(level) ? "橙色" : "黄色");
            a.put("content", (w.getEnterpriseName() != null ? w.getEnterpriseName() : "") + "：" + (w.getContent() != null ? w.getContent() : ""));
            alerts.add(a);
        }

        // 口岸覆盖率 = 有通关记录的口岸数 / 总口岸数
        long activePorts = portList.stream().filter(m -> ((Number) m.get("count")).intValue() > 0).count();
        String portCoverage = ports.size() > 0
            ? String.format("%.1f%%", activePorts * 100.0 / ports.size()) : "0%";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalEnterprises", totalEnterprises);
        data.put("todayAlerts", todayAlerts);
        data.put("highRiskCount", highRiskCount);
        data.put("portCoverage", portCoverage);
        data.put("ports", portList);
        data.put("highRiskList", topHighRisk);
        data.put("alerts", alerts);
        return ApiResponse.success(data);
    }
}
