package com.risk.controller;

import com.risk.dto.ApiResponse;
import com.risk.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器 - 提供登录接口
 *
 * 安全说明（修复「任意用户名密码都能登录」的 Blocker）：
 * - 管理员账号密码通过 BCrypt 哈希校验，密码不以明文比对。
 * - 校验失败一律拒绝并返回错误，不再无差别放行。
 * - 提供 app.auth.demo 开关（默认 false）：仅演示环境可开启，
 *   开启后任意账号可登录但只获得 user 角色（无管理写权限），管理员仍走 BCrypt 校验。
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private TokenService tokenService;

    /** 演示模式开关：默认关闭。演示时可设环境变量 APP_AUTH_DEMO=true 开启 */
    @Value("${app.auth.demo:false}")
    private boolean demoMode;

    @Value("${app.auth.admin-username:admin}")
    private String adminUsername;

    /** 管理员密码的 BCrypt 哈希（对应明文 123456，生产请更换并重新生成哈希） */
    @Value("${app.auth.admin-password-hash:}")
    private String adminPasswordHash;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录接口
     * POST /api/login
     * body: { "username": "admin", "password": "123456" }
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return ApiResponse.error("用户名和密码不能为空");
        }

        // 管理员：BCrypt 哈希校验
        if (adminUsername.equals(username) && verifyAdminPassword(password)) {
            return buildLoginResponse(username, "admin");
        }

        // 演示模式：允许任意账号登录，但仅授予 user 角色（无管理写权限）
        if (demoMode) {
            return buildLoginResponse(username, "user");
        }

        // 校验失败一律拒绝，绝不无差别放行
        return ApiResponse.error("用户名或密码错误");
    }

    /** BCrypt 校验管理员密码，哈希未配置或格式非法时安全地返回 false */
    private boolean verifyAdminPassword(String rawPassword) {
        if (adminPasswordHash == null || adminPasswordHash.isEmpty()) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, adminPasswordHash);
        } catch (IllegalArgumentException e) {
            // 配置的哈希格式非法，视为校验失败
            return false;
        }
    }

    private ApiResponse<Map<String, Object>> buildLoginResponse(String username, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenService.issue(username, role));
        data.put("username", username);
        data.put("role", role);
        return ApiResponse.success(data);
    }

    /**
     * 登出接口
     * POST /api/logout
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.success("登出成功");
    }
}
