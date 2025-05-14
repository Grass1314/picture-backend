package com.grass.picturebackend.aop;

import com.grass.picturebackend.manager.auth.StpInterfaceImpl2;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author Mr.Liuxq
 * @description: 请求结束后清理切面
 * @date 2025年05月13日 15:28
 */
@Aspect
@Component
public class RequestCleanupAspect {

    /**
     * 在请求处理完成后清理 ThreadLocal 变量
     */
    @AfterReturning(pointcut = "execution(* com.grass.picturebackend.controller..*.*(..))", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        // 调用清理方法
        StpInterfaceImpl2.clearAuthContext();
    }
}
