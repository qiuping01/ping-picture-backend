package com.ping.pingpicture.infrastructure.common;

import com.ping.pingpicture.infrastructure.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应封装类
 *
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {

    private T data;

    private int code;

    private String message;

    /**
     * 自定义响应内容
     *
     * @param code
     * @param data
     * @param message
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 只需要返回数据和状态码
     *
     * @param code
     * @param data
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 错误响应
     *
     * @param errorCode
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
