package com.risk.controller;
import com.risk.dto.ApiResponse;
import com.risk.entity.DisputeCase;
import com.risk.mapper.DisputeCaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dispute")
@CrossOrigin
public class DisputeCaseController {
    @Autowired private DisputeCaseMapper mapper;
    @GetMapping("/list")
    public ApiResponse<Map<String,Object>> list(@RequestParam(defaultValue="") String type, @RequestParam(defaultValue="") String keyword, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        List<DisputeCase> list;
        if (type != null && !type.isEmpty()) {
            list = mapper.findByType(type, keyword, (page-1)*pageSize, pageSize);
        } else {
            list = mapper.search(keyword, (page-1)*pageSize, pageSize);
        }
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", mapper.countAll());
        return ApiResponse.success(data);
    }
    @PostMapping("/match")
    public ApiResponse<List<DisputeCase>> match(@RequestBody Map<String,String> params) {
        String userTC = params.getOrDefault("tradeCategory", "").trim();
        String userCO = params.getOrDefault("countries", "").trim();
        // 至少需一个输入
        if (userTC.isEmpty() && userCO.isEmpty()) {
            return ApiResponse.success(new ArrayList<>());
        }
        // 1) 候选查询（宽松 OR 匹配）
        List<DisputeCase> candidates = mapper.findCandidates(userTC, userCO);
        // 2) 多维加权评分 → 阈值过滤 → 相似度+日期排序 → Top10
        final double threshold = 20.0;
        List<DisputeCase> ranked = candidates.stream()
                .peek(c -> c.setSimilarity(computeSimilarity(c, userTC, userCO)))
                .filter(c -> c.getSimilarity() != null && c.getSimilarity() >= threshold)
                .sorted(Comparator
                        .comparingDouble(DisputeCase::getSimilarity).reversed()
                        .thenComparing(DisputeCase::getPublishDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());
        return ApiResponse.success(ranked);
    }
    @GetMapping("/{id}")
    public ApiResponse<DisputeCase> get(@PathVariable Long id) {
        return ApiResponse.success(mapper.findById(id));
    }
    @PostMapping
    public ApiResponse<String> add(@RequestBody DisputeCase caseItem) {
        mapper.insert(caseItem);
        return ApiResponse.success("添加成功");
    }

    /** 多维度加权相似度 0-100
     *  贸易品类(主信号) 45 | 标题含贸易品类 +15 | 摘要含贸易品类 +5
     *  涉及国家 45（按用户输入国家命中比例）
     *  时效 0-5（≤12月+5，≤24月+2） */
    private double computeSimilarity(DisputeCase c, String userTC, String userCO) {
        double score = 0;
        String cTC = str(c.getTradeCategory());
        String cCO = str(c.getCountries());
        String cTitle = str(c.getTitle());
        String cSummary = str(c.getSummary());

        if (!userTC.isEmpty()) {
            if (containsCI(cTC, userTC) || (!cTC.isEmpty() && containsCI(userTC, cTC))) score += 45;
            if (containsCI(cTitle, userTC)) score += 15;
            if (containsCI(cSummary, userTC)) score += 5;
        }
        if (!userCO.isEmpty()) {
            // 支持多国家输入：越南,泰国 / 越南 泰国 / 越南、泰国
            String[] userCountries = userCO.split("[,，;；、\\s]+");
            int matched = 0, total = 0;
            for (String uc : userCountries) {
                if (uc.isEmpty()) continue;
                total++;
                if (containsCI(cCO, uc)) matched++;
            }
            if (total > 0) score += 45.0 * matched / total;
        }
        try {
            if (c.getPublishDate() != null && !c.getPublishDate().isEmpty()) {
                long months = ChronoUnit.MONTHS.between(
                        LocalDate.parse(c.getPublishDate()), LocalDate.now());
                if (months <= 12) score += 5;
                else if (months <= 24) score += 2;
            }
        } catch (Exception ignored) {}
        return Math.min(100.0, Math.round(score * 10) / 10.0);
    }
    private static String str(String s) { return s == null ? "" : s; }
    private static boolean containsCI(String haystack, String needle) {
        return !needle.isEmpty() && haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
