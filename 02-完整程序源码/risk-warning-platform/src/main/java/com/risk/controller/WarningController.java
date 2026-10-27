package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.entity.WarningMessage;
import com.risk.mapper.WarningMessageMapper;
import com.risk.mapper.WarningSubscriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/warning")
public class WarningController {

    @Autowired
    private WarningMessageMapper warningMessageMapper;
    @Autowired
    private WarningSubscriptionMapper subscriptionMapper;

    /**
     * 预警列表 GET /api/warning/list?page=1&pageSize=10
     * 【面试】字段映射：DB列名(snake_case) → API字段(camelCase)，createTime转为毫秒时间戳
     */
    @GetMapping("/list")
    public ApiResponse<Map<String,Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "statusPriority") String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String level,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String startDate,
            @RequestParam(defaultValue = "") String endDate) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 200) pageSize = 200;
        int offset = (page - 1) * pageSize;
        List<WarningMessage> list = warningMessageMapper.findByPage(
                offset, pageSize, sortField, sortOrder, type, level, status, startDate, endDate);
        List<Map<String,Object>> mappedList = new ArrayList<>();
        for (WarningMessage w : list) {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(w.getId()));
            item.put("enterpriseId", w.getCreditCode());       // 兼容API.md的enterpriseId
            item.put("creditCode", w.getCreditCode());          // 保留原始字段
            item.put("enterpriseName", w.getEnterpriseName());
            item.put("warningLevel", w.getWarningLevel());      // 高风险/中风险/低风险
            item.put("content", w.getContent());                // 兼容API.md的content
            item.put("warningContent", w.getContent());          // 保留原始字段
            item.put("warningType", w.getWarningType());
            item.put("dataSource", w.getDataSource());
            item.put("status", w.getStatus() != null ? w.getStatus() : "未处理"); // 处理状态
            item.put("handler", w.getHandler());
            item.put("handleTime", w.getHandleTime() != null ? w.getHandleTime().getTime() : 0);
            item.put("handleResult", w.getHandleResult());
            // createTime转为毫秒时间戳（兼容API.md要求）
            item.put("createTime", w.getCreateTime() != null ? w.getCreateTime().getTime() : 0);
            mappedList.add(item);
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("list", mappedList);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", warningMessageMapper.countFiltered(type, level, status, startDate, endDate));
        return ApiResponse.success(result);
    }

    /**
     * 订阅预警 POST /api/warning/subscribe
     * 【面试】兼容enterpriseId和creditCode两种参数名
     */
    @PostMapping("/subscribe")
    public ApiResponse<String> subscribe(@RequestBody Map<String,String> body) {
        String openId = body.getOrDefault("openId", "default_user");
        // 兼容API.md的enterpriseId和原creditCode
        String creditCode = body.getOrDefault("enterpriseId", body.get("creditCode"));
        if (creditCode == null) return ApiResponse.error("参数不完整");
        int exists = subscriptionMapper.exists(openId, creditCode);
        if (exists > 0) return ApiResponse.success("已订阅");
        try {
            subscriptionMapper.insert(openId, creditCode);
        } catch (DuplicateKeyException e) {
            // 并发下由唯一约束 (open_id, credit_code) 兜底：另一请求已抢先插入，幂等返回
            return ApiResponse.success("已订阅");
        }
        return ApiResponse.success("订阅成功");
    }

    /**
     * 取消订阅 POST /api/warning/unsubscribe
     */
    @PostMapping("/unsubscribe")
    public ApiResponse<String> unsubscribe(@RequestBody Map<String,String> body) {
        String openId = body.getOrDefault("openId", "default_user");
        String creditCode = body.getOrDefault("enterpriseId", body.get("creditCode"));
        if (creditCode == null) return ApiResponse.error("参数不完整");
        subscriptionMapper.delete(openId, creditCode);
        return ApiResponse.success("已取消订阅");
    }

    /** 订阅状态 GET /api/warning/status?enterpriseId=xxx */
    @GetMapping("/status")
    public ApiResponse<Map<String,Object>> status(
            @RequestParam String enterpriseId,
            @RequestParam(defaultValue = "default_user") String openId) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("subscribed", subscriptionMapper.exists(openId, enterpriseId) > 0);
        return ApiResponse.success(result);
    }

    /**
     * 我的订阅列表 GET /api/warning/subscribed-list
     * 【面试】返回订阅企业列表，供跨设备同步使用
     */
    @GetMapping("/subscribed-list")
    public ApiResponse<List<Map<String,Object>>> subscribedList(
            @RequestParam(defaultValue = "default_user") String openId) {
        return ApiResponse.success(subscriptionMapper.findSubscribedEnterprises(openId));
    }

    /** 切换处理状态 PUT /api/warning/{id}/status  body: {"status": "处理中/已处理/已关闭"}
     *  独立端点避免「全字段替换」式 update 把其他列置 NULL 触发 NOT NULL 约束 */
    @PutMapping("/{id}/status")
    public ApiResponse<String> updateStatus(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        if (status == null || status.isEmpty()) {
            return ApiResponse.error("状态值不能为空");
        }
        int rows = warningMessageMapper.updateStatus(id, status);
        return rows > 0 ? ApiResponse.success("状态已更新为：" + status)
                        : ApiResponse.error("更新失败，预警可能不存在");
    }

    /**
     * 处理闭环 POST /api/warning/{id}/process
     * body: { handler, handleResult }
     * 单次原子写：status='已处理' + handler + handle_result + handle_time=NOW()
     */
    @PostMapping("/{id}/process")
    public ApiResponse<Map<String,Object>> processWarning(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        String handler = body.get("handler") == null ? "" : String.valueOf(body.get("handler"));
        String handleResult = body.get("handleResult") == null ? "" : String.valueOf(body.get("handleResult"));
        if (handler.isEmpty()) return ApiResponse.error("处理人不能为空");
        if (handleResult.isEmpty()) return ApiResponse.error("处理说明不能为空");
        int rows = warningMessageMapper.processWarning(id, handler, handleResult);
        if (rows <= 0) return ApiResponse.error("处理失败，预警可能不存在");
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("handler", handler);
        data.put("handleResult", handleResult);
        data.put("handleTime", java.time.LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }
}
