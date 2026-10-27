package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.FraudAlert;
import com.risk.mapper.FraudAlertMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/fraud-alerts")
@CrossOrigin
public class FraudAlertController {

    @Autowired
    private FraudAlertMapper fraudAlertMapper;

    /** 查询企业预警 GET /api/fraud-alerts/{creditCode} */
    @GetMapping("/{creditCode}")
    public ApiResponse<List<FraudAlert>> getAlerts(@PathVariable String creditCode) {
        List<FraudAlert> alerts = fraudAlertMapper.selectByCreditCode(creditCode);
        return ApiResponse.success(alerts);
    }

    /** 获取未确认预警数量 GET /api/fraud-alerts/unconfirmed-count */
    @GetMapping("/unconfirmed-count")
    public ApiResponse<Integer> getUnconfirmedCount() {
        int count = fraudAlertMapper.countUnconfirmed();
        return ApiResponse.success(count);
    }

    /** 分页列表 GET /api/fraud-alerts/list?page=1&pageSize=10 */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "statusPriority") String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 200) pageSize = 200;
        List<FraudAlert> alerts = fraudAlertMapper.findAll((page - 1) * pageSize, pageSize, sortField, sortOrder);
        if (alerts == null) alerts = new ArrayList<>();
        int total = fraudAlertMapper.countAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (FraudAlert a : alerts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getEnterpriseName());
            m.put("fraudType", a.getFraudType());
            m.put("probability", (a.getRiskProbability() != null ? (int)(a.getRiskProbability().doubleValue() * 100) : 0) + "%");
            m.put("riskLevel", a.getRiskLevel());
            m.put("content", a.getAlertContent());
            m.put("window", a.getPredictionPeriod());
            m.put("status", a.getIsConfirmed() != null && a.getIsConfirmed() == 1 ? "已处理" :
                    a.getIsConfirmed() != null && a.getIsConfirmed() == 2 ? "已排除" : "待核实");
            // 处置闭环字段（处理人/动作/意见/时间）
            m.put("handler", a.getHandler());
            m.put("handleAction", a.getHandleAction());
            m.put("handleResult", a.getHandleResult());
            m.put("handleTime", a.getHandleTime() != null ? a.getHandleTime().toString() : "");
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        return ApiResponse.success(data);
    }

    /** 更新状态 PUT /api/fraud-alerts/{id}/status */
    @PutMapping("/{id}/status")
    public ApiResponse<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String statusStr = (String) body.get("status");
        int status = "已确认".equals(statusStr) ? 1 : 2;
        int rows = fraudAlertMapper.updateStatus(id, status);
        return rows > 0 ? ApiResponse.success("更新成功") : ApiResponse.error("更新失败");
    }

    /** 确认预警 PUT /api/fraud-alerts/{id}/confirm（兼容旧接口） */
    @PutMapping("/{id}/confirm")
    public ApiResponse<String> confirmAlert(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = (Integer) body.get("status");
        String handler = (String) body.get("handler");
        String result = (String) body.get("result");
        int rows = fraudAlertMapper.updateConfirmation(id, status, handler, result, java.time.LocalDateTime.now());
        return rows > 0 ? ApiResponse.success("确认成功") : ApiResponse.error("确认失败");
    }

    /**
     * 处置闭环 POST /api/fraud-alerts/process
     * body: { id, handler, action, remark }
     * 单次原子写：is_confirmed=1 + handler + handle_action + handle_result + handle_time=NOW()
     * （独立端点避免全字段替换踩 NOT NULL 雷，同时保证处置四要素完整记录）
     */
    @PostMapping("/process")
    public ApiResponse<Map<String, Object>> processAlert(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) return ApiResponse.error("id 不能为空");
        Long id = Long.valueOf(idObj.toString());
        String handler = str(body.get("handler"));
        String action = str(body.get("action"));
        String remark = str(body.get("remark"));
        if (handler.isEmpty()) return ApiResponse.error("处理人不能为空");
        if (action.isEmpty()) return ApiResponse.error("请选择处置动作");
        int rows = fraudAlertMapper.processAlert(id, handler, action, remark);
        if (rows <= 0) return ApiResponse.error("处置失败，预警可能不存在");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("handler", handler);
        data.put("action", action);
        data.put("remark", remark);
        data.put("handleTime", java.time.LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }

    /**
     * 处置统计 GET /api/fraud-alerts/stats
     * 返回：今日新增 / 待核实 / 已处理 / 处理率（单次 SQL 聚合）
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> processStats() {
        Map<String, Object> raw = fraudAlertMapper.getProcessStats();
        if (raw == null) raw = new HashMap<>();
        long todayNew = toLong(raw.get("todayNew"));
        long pending = toLong(raw.get("pending"));
        long processed = toLong(raw.get("processed"));
        long total = toLong(raw.get("total"));
        // 处理率 = 已处理 / (已处理 + 待核实)，避免总数里的"已排除"混入分母
        long denominator = processed + pending;
        double rate = denominator > 0 ? Math.round((processed * 10000.0 / denominator)) / 100.0 : 0.0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayNew", todayNew);
        data.put("pending", pending);
        data.put("processed", processed);
        data.put("total", total);
        data.put("processingRate", rate);
        return ApiResponse.success(data);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }
}
