package com.codegym.logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class Logger {

    // Pointcut định vị đến TẤT CẢ các phương thức thuộc các class trong package
    // com.codegym.service
    @Pointcut("execution(* com.codegym.service..*.*(..))")
    public void serviceMethods() {
    }

    // Advice GHI LOG KHI CÓ NGOẠI LỆ NẾM RA
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "e")
    public void logError(JoinPoint joinPoint, Throwable e) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = Arrays.toString(joinPoint.getArgs());

        System.err.println("==================== [AOP ERROR LOG] ====================");
        System.err.println("-> Class      : " + className);
        System.err.println("-> Method     : " + methodName);
        System.err.println("-> Arguments  : " + args);
        System.err.println("-> Exception  : " + e.getMessage());
        System.err.println("=========================================================");
    }

    // Advice GHI LOG KHI PHƯƠNG THỨC THỰC THI THÀNH CÔNG (Mở rộng thêm)
    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logSuccess(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        System.out.println("-------------------- [AOP INFO LOG] --------------------");
        System.out.println("-> Executed   : " + className + "." + methodName + "() thành công.");
        System.out.println("--------------------------------------------------------");
    }
}