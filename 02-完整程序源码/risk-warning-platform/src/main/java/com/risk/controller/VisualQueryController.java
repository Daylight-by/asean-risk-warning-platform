package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.dto.VisualQueryRequest;
import com.risk.security.AuthInterceptor;
import com.risk.service.DashboardService;
import com.risk.service.VisualQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 可视化查询（轻量低代码）控制器。
 * GET  /api/query/sources      —— 返回可查询的数据源与字段元信息
 * POST /api/query/execute      —— 执行自助查询，返回表格数据或图表聚合结果
 * GET  /api/query/dashboards   —— 当前用户已保存的看板列表
 * POST /api/query/dashboards   —— 保存一个看板
 * DELETE /api/query/dashboards/{id} —— 删除一个看板
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin
public class VisualQueryController {

    @Autowired
    private VisualQueryService service;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/sources")
    public ApiResponse<Object> sources() {
        return ApiResponse.success(service.listSources());
    }

    @PostMapping("/execute")
    public ApiResponse<Object> execute(@RequestBody VisualQueryRequest req) {
        try {
            return ApiResponse.success(service.execute(req));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/dashboards")
    public ApiResponse<Object> listDashboards(HttpServletRequest request) {
        String username = currentUsername(request);
        if (username == null) {
            return ApiResponse.error("未登录，无法访问看板");
        }
        List<Map<String, Object>> list = dashboardService.list(username);
        return ApiResponse.success(list);
    }

    @PostMapping("/dashboards")
    public ApiResponse<Object> saveDashboard(HttpServletRequest request,
                                             @RequestBody Map<String, Object> body) {
        String username = currentUsername(request);
        if (username == null) {
            return ApiResponse.error("未登录，无法保存看板");
        }
        String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
        Object config = body.get("config");
        Map<String, Object> saved = dashboardService.save(username, name, config);
        return ApiResponse.success(saved);
    }

    @DeleteMapping("/dashboards/{id}")
    public ApiResponse<Object> deleteDashboard(HttpServletRequest request,
                                               @PathVariable("id") Long id) {
        String username = currentUsername(request);
        if (username == null) {
            return ApiResponse.error("未登录，无法删除看板");
        }
        dashboardService.delete(username, id);
        return ApiResponse.success("ok");
    }

    /**
     * 从服务端会话（token）解析出的身份中获取当前用户名。
     * 身份由 AuthInterceptor 校验 token 后写入 request attribute，绝不信任客户端请求头。
     */
    private String currentUsername(HttpServletRequest request) {
        Object u = request.getAttribute(AuthInterceptor.ATTR_USERNAME);
        return u == null ? null : String.valueOf(u);
    }
}
