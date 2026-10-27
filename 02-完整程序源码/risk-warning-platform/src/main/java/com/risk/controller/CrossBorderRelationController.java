package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.CrossBorderRelation;
import com.risk.service.CrossBorderRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/relations")
@CrossOrigin
public class CrossBorderRelationController {

    @Autowired
    private CrossBorderRelationService crossBorderRelationService;

    /** 查询企业关联关系 GET /api/relations/{creditCode} */
    @GetMapping("/{creditCode}")
    public ApiResponse<List<CrossBorderRelation>> getRelations(@PathVariable String creditCode) {
        List<CrossBorderRelation> relations = crossBorderRelationService.getRelationsByCreditCode(creditCode);
        return ApiResponse.success(relations);
    }

    /** 查询高风险关联关系 GET /api/relations/high-risk/{creditCode} */
    @GetMapping("/high-risk/{creditCode}")
    public ApiResponse<List<CrossBorderRelation>> getHighRiskRelations(@PathVariable String creditCode) {
        List<CrossBorderRelation> relations = crossBorderRelationService.getHighRiskRelations(creditCode);
        return ApiResponse.success(relations);
    }

    /** 获取关联关系统计 GET /api/relations/stats */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = crossBorderRelationService.getRelationStats();
        return ApiResponse.success(stats);
    }

    /** 获取图谱数据(节点+边+关联列表) GET /api/relations/graph/{creditCode} */
    @GetMapping("/graph/{creditCode}")
    public ApiResponse<Map<String, Object>> getGraph(@PathVariable String creditCode) {
        List<CrossBorderRelation> relations = crossBorderRelationService.getRelationsByCreditCode(creditCode);
        if (relations == null) relations = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();

        // 构建节点和边
        Set<String> nodeNames = new LinkedHashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        List<Map<String, Object>> relationList = new ArrayList<>();

        // 添加源企业节点
        String sourceName = relations.isEmpty() ? creditCode : relations.get(0).getSourceEnterprise();
        nodeNames.add(sourceName);
        Map<String, Object> sourceNode = new LinkedHashMap<>();
        sourceNode.put("name", sourceName);
        sourceNode.put("category", 0);
        sourceNode.put("symbolSize", 50);
        sourceNode.put("riskLevel", "中风险");
        nodes.add(sourceNode);

        for (CrossBorderRelation r : relations) {
            String target = r.getTargetEntityName();
            if (!nodeNames.contains(target)) {
                nodeNames.add(target);
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("name", target);
                node.put("category", "中国".equals(r.getTargetCountry()) ? 0 : 1);
                node.put("symbolSize", 30 + (r.getRelationDepth() != null ? (3 - r.getRelationDepth()) * 5 : 0));
                node.put("riskLevel", r.getRiskLevel() != null ? r.getRiskLevel() : "低风险");
                nodes.add(node);
            }
            Map<String, Object> link = new LinkedHashMap<>();
            link.put("source", sourceName);
            link.put("target", target);
            link.put("relation", r.getRelationType());
            String color = "高".equals(r.getRiskLevel()) ? "#F56C6C" : "中".equals(r.getRiskLevel()) ? "#E6A23C" : "#67C23A";
            Map<String, Object> lineStyle = new LinkedHashMap<>();
            lineStyle.put("color", color);
            link.put("lineStyle", lineStyle);
            links.add(link);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("entity", target);
            row.put("country", r.getTargetCountry());
            row.put("relationType", r.getRelationType());
            row.put("level", r.getRelationDepth());
            row.put("riskLevel", r.getRiskLevel());
            row.put("riskTags", r.getRiskTag());
            relationList.add(row);
        }

        result.put("nodes", nodes);
        result.put("links", links);
        result.put("relations", relationList);
        return ApiResponse.success(result);
    }
}
