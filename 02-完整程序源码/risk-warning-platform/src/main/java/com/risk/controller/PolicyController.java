package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.mapper.PolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    @Autowired
    private PolicyMapper policyMapper;

    /**
     * 搜索政策 GET /api/policy/search?keyword=&country=&page=1&pageSize=10
     * 【面试】DB列名(snake_case) → API字段(camelCase)映射
     */
    @GetMapping("/search")
    public ApiResponse<Map<String,Object>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String country,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 200) pageSize = 200;
        int offset = (page - 1) * pageSize;
        List<Map<String,Object>> rawList = policyMapper.searchPage(keyword, country, offset, pageSize);
        // 映射DB列名到API友好字段名
        List<Map<String,Object>> list = new ArrayList<>();
        for (Map<String,Object> row : rawList) {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id") != null ? String.valueOf(row.get("id")) : "");
            item.put("title", row.get("policy_title"));             // 兼容API.md
            item.put("policyTitle", row.get("policy_title"));        // 保留原始字段
            item.put("category", row.get("policy_category"));        // 分类
            item.put("policyCategory", row.get("policy_category"));  // 保留原始字段
            item.put("country", row.get("target_country"));          // 兼容API.md
            item.put("targetCountry", row.get("target_country"));    // 保留原始字段
            item.put("summary", row.get("policy_content"));          // 兼容API.md
            item.put("policyContent", row.get("policy_content"));    // 保留原始字段
            item.put("publishDate", row.get("publish_date") != null ? row.get("publish_date").toString() : "");
            item.put("source", row.get("data_source") != null ? row.get("data_source") : "官方发布");
            item.put("dataSource", row.get("data_source") != null ? row.get("data_source") : "官方发布"); // 前端发文部门字段
            item.put("isActive", row.get("is_active") != null ? ((Number)row.get("is_active")).intValue() == 1 : true); // 启用状态
            item.put("link", row.get("link") != null ? row.get("link") : ""); // 附件链接
            item.put("tags", new ArrayList<>());                     // 政策表无tags列，返回空数组
            list.add(item);
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", policyMapper.countSearch(keyword, country));
        return ApiResponse.success(result);
    }

    /** 按分类查询 GET /api/policy/category?category=关税 */
    @GetMapping("/category")
    public ApiResponse<List<Map<String,Object>>> byCategory(@RequestParam String category) {
        return ApiResponse.success(policyMapper.findByCategory(category));
    }

    /** 新增政策 POST /api/policy */
    @PostMapping
    public ApiResponse<String> create(@RequestBody Map<String,Object> policy) {
        int rows = policyMapper.insert(policy);
        return rows > 0 ? ApiResponse.success("新增成功") : ApiResponse.error("新增失败");
    }

    /** 更新政策 PUT /api/policy/{id} */
    @PutMapping("/{id}")
    public ApiResponse<String> update(@PathVariable Long id, @RequestBody Map<String,Object> policy) {
        policy.put("id", id);
        int rows = policyMapper.update(policy);
        return rows > 0 ? ApiResponse.success("更新成功") : ApiResponse.error("更新失败");
    }

    /** 切换启用/禁用状态 PUT /api/policy/{id}/status  body: {"active": true/false}
     *  独立端点避免「全字段替换」式 update 把其他列置 NULL 触发 NOT NULL 约束 */
    @PutMapping("/{id}/status")
    public ApiResponse<String> updateStatus(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        Object v = body.get("active");
        int isActive = (v instanceof Boolean) ? ((Boolean) v ? 1 : 0)
                : (v instanceof Number ? ((Number) v).intValue() : 1);
        int rows = policyMapper.updateStatus(id, isActive);
        return rows > 0 ? ApiResponse.success(isActive == 1 ? "已启用" : "已禁用")
                        : ApiResponse.error("状态更新失败");
    }

    /** 删除政策 DELETE /api/policy/{id} */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        int rows = policyMapper.deleteById(id);
        return rows > 0 ? ApiResponse.success("删除成功") : ApiResponse.error("删除失败");
    }
}