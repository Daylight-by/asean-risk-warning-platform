package com.risk.exception;

import com.risk.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

/**
 * 全局异常处理：将任意未捕获异常统一包装为 { code, message, data } 结构，
 * 避免将堆栈暴露给前端，并始终保持与前端 api.js 约定的响应格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", msg);
        return ApiResponse.error(msg);
    }

    /** 缺少必需请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = "缺少必需参数: " + ex.getParameterName();
        log.warn(msg);
        return ApiResponse.error(msg);
    }

    /** 请求体无法解析（如 JSON 格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败", ex);
        return ApiResponse.error("请求体格式错误");
    }

    /** 非法参数（如分页越界、业务断言） */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegal(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ApiResponse.error(ex.getMessage());
    }

    /** 其余所有未预期异常：返回 500 但隐藏堆栈，仅暴露安全提示 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleAll(Exception ex) {
        log.error("服务器内部异常", ex);
        return ApiResponse.error("服务器内部异常，请稍后重试");
    }
}
