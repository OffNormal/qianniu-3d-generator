package org.example.types.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用响应结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    
    private String code;
    private String message;
    private T data;
    
    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code("0000")
                .message("成功")
                .data(data)
                .build();
    }
    
    public static <T> Response<T> success() {
        return success(null);
    }
    
    public static <T> Response<T> fail(String code, String message) {
        return Response.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
    
    public static <T> Response<T> fail(String message) {
        return fail("9999", message);
    }
    
    public boolean isSuccess() {
        return "0000".equals(code);
    }
}