package com.risk.dto;

import lombok.Data;

/**
 * 通用API响应封装
 */
@Data
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setCode(0);
        res.setMessage("ok");
        res.setData(data);
        return res;
    }

    public static <T> ApiResponse<T> error(String msg) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setCode(500);
        res.setMessage(msg);
        return res;
    }
}
