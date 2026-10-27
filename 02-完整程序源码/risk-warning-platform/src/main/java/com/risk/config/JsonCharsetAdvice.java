package com.risk.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;

/**
 * 统一给所有 JSON 响应头补上 charset=UTF-8。
 *
 * <p>问题背景：Spring Boot 3.3 + Jackson 在某些情况下返回 {@code Content-Type: application/json}
 * （不带 charset）。HTTP 规范规定这种情况默认按 ISO-8859-1 解码，
 * 浏览器/axios 会把 UTF-8 字节流当成 Latin-1，导致中文/越南语/泰语显示成乱码（æŠ¥å…³å• 这种）。
 * 英文不受影响是因为 ASCII 在两种编码中字节相同。</p>
 *
 * <p>本 Advice 在响应写出前检查并补上 {@code ;charset=UTF-8}，
 * 优先级最高，覆盖 Jackson 默认行为。</p>
 */
@RestControllerAdvice
public class JsonCharsetAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        MediaType ct = response.getHeaders().getContentType();
        if (ct == null) {
            response.getHeaders().setContentType(
                new MediaType("application", "json", StandardCharsets.UTF_8));
            return body;
        }
        String type = ct.toString().toLowerCase();
        // 仅处理 JSON 响应；非 JSON（如文件下载、HTML）不强制改写
        boolean isJson = type.contains("application/json") || type.contains("+json");
        if (!isJson) return body;

        if (ct.getCharset() == null) {
            // 缺 charset —— 补上 UTF-8
            response.getHeaders().setContentType(
                new MediaType(ct.getType(), ct.getSubtype(), StandardCharsets.UTF_8));
        }
        return body;
    }
}
