package com.ping.pingpicture.infrastructure.common;

import com.ping.pingpicture.infrastructure.exception.ErrorCode;

/**
 * 响应工具类
 */
public class ResultUtils {

    /**
     * 有数据成功 - 使用具体类型 <T>
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "success");
    }

    /**
     * 失败 - 使用 ErrorCode
     *
     * @param errorCode 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败 - 自定义错误码和消息
     *
     * @param code      错误码
     * @param message   错误消息
     * @return          响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败 - 使用 ErrorCode + 自定义消息
     *
     * @param errorCode 错误码
     * @param message   错误消息
     * @return          响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

}

