package com.risk.security;

import com.risk.dto.ApiResponse;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 统一认证鉴权拦截器。
 *
 * 职责：
 * 1. 从 Authorization: Bearer <token> 头解析身份（TokenService 校验签名与过期），
 *    并把 username / role 写入 request attribute，供 Controller 读取（绝不信任客户端自定义头）。
 * 2. 对管理端写操作（POST/PUT/DELETE）强制要求登录；对高危管理接口额外要求 admin 角色。
 * 3. 对小程序等无登录态的公开接口放行（白名单），避免误伤 C 端功能。
 *
 * 放行规则（requireAuth = false）：
 * - 所有 GET 读接口（/api/query/dashboards 除外，看板涉及用户私有数据，需登录）。
 * - 白名单内的公开写接口（登录、小程序订阅/报告/合规体检/纠纷匹配/AI 助手等）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USERNAME = "authUsername";
    public static final String ATTR_ROLE = "authRole";

    @Autowired
    private TokenService tokenService;

    /** 无需登录即可访问的公开写接口（登录接口 + 小程序 C 端功能） */
    private static final Set<String> PUBLIC_WRITE_PATHS = Set.of(
            "/api/login",
            "/api/logout",
            "/api/warning/subscribe",
            "/api/warning/unsubscribe",
            "/api/enterprise/report",
            "/api/enterprise/compliance-check",
            "/api/dispute/match",
            "/api/ai/chat",
            "/api/ai/chat/stream"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // CORS 预检请求直接放行（由 CorsFilter 处理）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 1) 尝试解析身份（无论是否强制校验，都先解析，供 Controller 使用）
        TokenService.TokenIdentity identity = resolveIdentity(request);
        if (identity != null) {
            request.setAttribute(ATTR_USERNAME, identity.getUsername());
            request.setAttribute(ATTR_ROLE, identity.getRole());
        }

        // 2) 判断是否需要登录
        boolean isWrite = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
        boolean isDashboard = path.startsWith("/api/query/dashboards");
        boolean requireAuth = isDashboard || (isWrite && !PUBLIC_WRITE_PATHS.contains(path));

        if (requireAuth) {
            if (identity == null) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
                return false;
            }
            // 3) 高危管理接口需要 admin 角色
            if (isAdminOnly(path) && !"admin".equals(identity.getRole())) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "需要管理员权限");
                return false;
            }
        }
        return true;
    }

    /** 从 Authorization 头解析并校验 Token */
    private TokenService.TokenIdentity resolveIdentity(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return tokenService.validate(header.substring(7).trim());
    }

    /** 高危管理写操作：政策 / 国别风险权重 / 数据源同步配置，仅 admin 可操作 */
    private boolean isAdminOnly(String path) {
        return path.startsWith("/api/policy")
                || path.startsWith("/api/country-risk")
                || path.startsWith("/api/datasource")
                || path.startsWith("/api/system/thresholds");
    }

    /** 以 JSON 形式返回统一错误结构（与 ApiResponse 约定一致） */
    private void writeJson(HttpServletResponse response, int httpStatus, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(message);
        response.getOutputStream().write(JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }
}
