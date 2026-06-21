package com.club.common.aspect;

import com.club.common.annotation.Log;
import com.club.entity.ExceptionLog;
import com.club.entity.OperationLog;
import com.club.service.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private AdminLogService adminLogService;

    @Pointcut("@annotation(com.club.common.annotation.Log)")
    public void logPointCut() {}

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = point.proceed();
        long time = System.currentTimeMillis() - beginTime;
        saveOperationLog(point, time, 1);
        return result;
    }

    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Throwable e) {
        saveExceptionLog(joinPoint, e);
    }

    private void saveOperationLog(JoinPoint joinPoint, long time, int status) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog operLog = new OperationLog();
        Log logAnnotation = method.getAnnotation(Log.class);
        if (logAnnotation != null) {
            operLog.setOperation(logAnnotation.value());
        }

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        operLog.setMethod(className + "." + methodName + "()");

        Object[] args = joinPoint.getArgs();
        operLog.setParams(Arrays.toString(args));

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        operLog.setIp(request.getRemoteAddr());

        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        operLog.setUsername(username);

        operLog.setTime(time);
        operLog.setStatus(status);
        operLog.setCreateTime(LocalDateTime.now());
        adminLogService.saveOperationLog(operLog);
    }

    private void saveExceptionLog(JoinPoint joinPoint, Throwable e) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ExceptionLog exLog = new ExceptionLog();
        Log logAnnotation = method.getAnnotation(Log.class);
        if (logAnnotation != null) {
            exLog.setOperation(logAnnotation.value());
        }

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        exLog.setMethod(className + "." + methodName + "()");

        Object[] args = joinPoint.getArgs();
        exLog.setParams(Arrays.toString(args));

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        exLog.setIp(request.getRemoteAddr());

        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        exLog.setUsername(username);

        exLog.setExceptionName(e.getClass().getName());
        exLog.setExceptionMessage(e.getMessage());
        
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            stackTrace.append(element.toString()).append("\n");
        }
        exLog.setStackTrace(stackTrace.toString());
        
        exLog.setCreateTime(LocalDateTime.now());
        adminLogService.saveExceptionLog(exLog);
    }
}
