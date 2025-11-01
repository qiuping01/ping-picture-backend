package com.ping.pingpicturebackend.aop;

import com.ping.pingpicturebackend.annotation.AuthCheck;
import com.ping.pingpicturebackend.exception.BusinessException;
import com.ping.pingpicturebackend.exception.ErrorCode;
import com.ping.pingpicturebackend.model.entity.User;
import com.ping.pingpicturebackend.model.enums.UserRoleEnum;
import com.ping.pingpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.BatchUpdateException;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 获取需要校验的权限
        String mustRole = authCheck.mustRole();
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 2. 判断是否需要对应的权限,如果不需要权限，放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 3. 获取当前用户登录的信息
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 4. 判断是否需要对应的权限,如果不需要权限，放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 5. 以下的代码：必须有权限，才会通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 6. 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 7. 通过权限校验，放行
        return joinPoint.proceed();
    }
}
