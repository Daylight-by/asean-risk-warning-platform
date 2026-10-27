package com.risk.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 轻量级无状态 Token 服务（HMAC-SHA256 签名）。
 *
 * 设计说明：
 * - 不引入完整 JWT 库，用 JDK 自带的 javax.crypto 做 HMAC-SHA256 签名，零额外依赖。
 * - Token 结构：base64url(payload) + "." + base64url(hmacSha256(payload, secret))
 *   payload 为 JSON：{"u":用户名,"r":角色,"e":过期毫秒时间戳}
 * - 服务端无状态校验：重算签名比对 + 校验过期时间，不依赖 Redis/DB。
 *
 * 安全要点：
 * - 签名密钥从配置读取，生产必须通过环境变量覆盖默认值。
 * - 签名比对使用定长时间比较（MessageDigest.isEqual），防时序攻击。
 */
@Component
public class TokenService {

    private static final String HMAC_ALGO = "HmacSHA256";

    /** 签名密钥：生产环境务必通过环境变量 APP_AUTH_SECRET 覆盖 */
    @Value("${app.auth.token-secret:risk-platform-default-secret-change-me}")
    private String secret;

    /** Token 有效期（分钟） */
    @Value("${app.auth.token-expire-minutes:120}")
    private long expireMinutes;

    /**
     * 签发 Token
     */
    public String issue(String username, String role) {
        long expireAt = System.currentTimeMillis() + expireMinutes * 60_000L;
        JSONObject payload = new JSONObject();
        payload.put("u", username);
        payload.put("r", role);
        payload.put("e", expireAt);
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.toJSONString().getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }

    /**
     * 校验 Token，合法返回 TokenIdentity，否则返回 null。
     * 校验内容：签名完整性 + 未过期。
     */
    public TokenIdentity validate(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            int dot = token.lastIndexOf('.');
            if (dot <= 0 || dot == token.length() - 1) {
                return null;
            }
            String payload = token.substring(0, dot);
            String sig = token.substring(dot + 1);

            // 定长时间比较签名，防时序攻击
            byte[] expected = sign(payload).getBytes(StandardCharsets.UTF_8);
            byte[] actual = sig.getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                return null;
            }

            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JSONObject obj = JSON.parseObject(json);
            long expireAt = obj.getLongValue("e");
            if (expireAt < System.currentTimeMillis()) {
                return null; // 已过期
            }
            return new TokenIdentity(obj.getString("u"), obj.getString("r"));
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Token 签名失败", e);
        }
    }

    /** Token 中解析出的身份 */
    public static class TokenIdentity {
        private final String username;
        private final String role;

        public TokenIdentity(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
