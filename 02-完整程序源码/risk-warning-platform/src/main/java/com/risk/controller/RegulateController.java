package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.PortGeo;
import com.risk.mapper.EnterpriseMapper;
import com.risk.mapper.PortGeoMapper;
import com.risk.service.CrossBorderRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/regulate")
@CrossOrigin
public class RegulateController {

    @Autowired private EnterpriseMapper enterpriseMapper;
    @Autowired private PortGeoMapper portGeoMapper;
    @Autowired private CrossBorderRelationService crossBorderRelationService;

    /**
     * 政企协同监管 - 风险热力图
     * GET /api/regulate/heatmap
     * 接口15：每个口岸/区域的高风险企业数与风险强度（0-3）
     */
    @GetMapping("/heatmap")
    public ApiResponse<Map<String, Object>> heatmap() {
        // 统计各口岸高风险企业数（评分<=40视为高风险）
        List<Map<String, Object>> allWithScore = enterpriseMapper.findAllWithRiskScore();
        if (allWithScore == null) allWithScore = new ArrayList<>();
        Map<String, Integer> portHighRisk = new LinkedHashMap<>();
        for (Map<String, Object> m : allWithScore) {
            Object scoreObj = m.get("riskScore");
            if (scoreObj == null) continue;
            double score = ((Number) scoreObj).doubleValue();
            if (score <= 40) {
                String port = m.get("primaryPort") != null ? String.valueOf(m.get("primaryPort")) : "未知口岸";
                portHighRisk.put(port, portHighRisk.getOrDefault(port, 0) + 1);
            }
        }
        // 与口岸表对齐，未出现的高风险记0
        List<PortGeo> ports = portGeoMapper.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        Set<String> covered = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> e : portHighRisk.entrySet()) {
            int count = e.getValue();
            int level = count >= 5 ? 3 : count >= 3 ? 2 : count >= 1 ? 1 : 0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("region", e.getKey());
            item.put("level", level);
            item.put("count", count);
            list.add(item);
            covered.add(e.getKey());
        }
        if (ports != null) {
            for (PortGeo p : ports) {
                if (covered.contains(p.getPortName())) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("region", p.getPortName());
                item.put("level", 0);
                item.put("count", 0);
                list.add(item);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "项目演示样例");
        data.put("sourceNote", "当前结果用于比赛功能演示；正式接入后补充公共数据目录编号与授权依据。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }

    /**
     * 政企协同监管 - 高风险集群筛查
     * GET /api/regulate/clusters?region=&level=
     * 接口16：按区域（目标国/口岸）聚合关联企业集群
     */
    @GetMapping("/clusters")
    public ApiResponse<Map<String, Object>> clusters(
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String level) {
        Map<String, Object> relationStats = crossBorderRelationService.getRelationStats();
        List<Map<String, Object>> list = new ArrayList<>();
        if (relationStats != null) {
            for (Map.Entry<String, Object> e : relationStats.entrySet()) {
                String country = e.getKey();
                Object val = e.getValue();
                if (val == null) continue;
                int count = (val instanceof Number) ? ((Number) val).intValue()
                        : Integer.parseInt(String.valueOf(val));
                // 简化：关联企业数>=3视为high，>=1为medium，否则low
                String clusterLevel = count >= 3 ? "high" : count >= 1 ? "medium" : "low";
                if (!region.isEmpty() && !country.contains(region)) continue;
                if (!level.isEmpty() && !clusterLevel.equals(level)) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("region", country);
                item.put("count", count);
                item.put("level", clusterLevel);
                item.put("enterprises", Collections.singletonList(country + "相关关联主体"));
                list.add(item);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("dataStatus", "DEMO");
        data.put("sourceType", "项目演示样例");
        data.put("sourceNote", "当前结果用于比赛功能演示；正式接入后补充公共数据目录编号与授权依据。");
        data.put("desensitized", true);
        return ApiResponse.success(data);
    }
}
