package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.SettlementCredit;
import com.risk.service.SettlementCreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/settlements")
@CrossOrigin
public class SettlementCreditController {

    @Autowired
    private SettlementCreditService settlementCreditService;

    /** 查询结算历史 GET /api/settlements/{creditCode} */
    @GetMapping("/{creditCode}")
    public ApiResponse<List<SettlementCredit>> getSettlementHistory(@PathVariable String creditCode) {
        List<SettlementCredit> history = settlementCreditService.getSettlementHistory(creditCode);
        return ApiResponse.success(history);
    }

    /** 获取信用评分 GET /api/settlements/score/{creditCode} */
    @GetMapping("/score/{creditCode}")
    public ApiResponse<Double> getCreditScore(@PathVariable String creditCode) {
        Double score = settlementCreditService.getCreditScore(creditCode);
        return ApiResponse.success(score);
    }

    /** 获取所有异常记录 GET /api/settlements/abnormal */
    @GetMapping("/abnormal")
    public ApiResponse<List<SettlementCredit>> getAbnormalRecords() {
        List<SettlementCredit> records = settlementCreditService.getAbnormalRecords();
        return ApiResponse.success(records);
    }

    /** 分页列表 GET /api/settlements/list */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 200) pageSize = 200;
        List<SettlementCredit> records = settlementCreditService.findAll((page - 1) * pageSize, pageSize);
        if (records == null) records = new ArrayList<>();
        int total = settlementCreditService.countAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (SettlementCredit s : records) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getEnterpriseName());
            m.put("type", s.getSettlementCurrency() != null ? s.getSettlementCurrency() + "结算" : "人民币结算");
            m.put("amount", s.getSettlementAmount() != null ? s.getSettlementAmount().doubleValue() : 0);
            m.put("direction", s.getSettlementDirection());
            m.put("counterCountry", s.getTargetCountry());
            m.put("date", s.getSettlementDate() != null ? s.getSettlementDate().toString() : "");
            m.put("abnormal", s.getIsAbnormal() != null && s.getIsAbnormal() == 1);
            m.put("creditScore", s.getCreditScore());
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        return ApiResponse.success(data);
    }

    /** 结算趋势 GET /api/settlements/trend */
    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend() {
        List<Map<String, Object>> dbData = settlementCreditService.monthlyTrend();
        List<Map<String, Object>> list = new ArrayList<>();
        if (dbData != null) {
            for (Map<String, Object> row : dbData) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("month", row.get("month"));
                item.put("exportAmount", row.get("exportAmount"));
                item.put("importAmount", row.get("importAmount"));
                list.add(item);
            }
        }
        return ApiResponse.success(list);
    }
}
