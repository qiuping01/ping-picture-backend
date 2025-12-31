package com.ping.pingpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.ping.pingpicturebackend.common.BaseResponse;
import com.ping.pingpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ping.pingpicturebackend.exception.ErrorCode.SYSTEM_ERROR;

/**
 * 全局异常处理器
 */
@RestControllerAdvice // 环绕切面，可以在该类中进行切面编程、切点
@Slf4j
public class GlobalExceptionHandler {

    // 切点
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("业务异常 BusinessException", e);
        return ResultUtils.error(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("运行时异常 RuntimeException", e);
        return ResultUtils.error(SYSTEM_ERROR,SYSTEM_ERROR.getMessage());
    }

    /**
     * 处理所有其他异常（可选添加）
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> exceptionHandler(Exception e) {
        log.error("系统异常 Exception", e);
        return ResultUtils.error(SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }

    @ExceptionHandler(NotRoleException.class)
    public BaseResponse<?> notRoleExceptionHandler(RuntimeException e) {
        log.error("NotRoleException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "无权限");
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(RuntimeException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "未登录");
    }
}
