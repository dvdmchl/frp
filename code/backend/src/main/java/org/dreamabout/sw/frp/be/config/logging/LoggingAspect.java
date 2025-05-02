package org.dreamabout.sw.frp.be.config.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.dreamabout.sw.frp.be.domain.FrpUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(@org.springframework.stereotype.Service *) " +
            "|| within(@org.springframework.stereotype.Controller *)" +
            "|| execution(* org.dreamabout.sw.frp.be..repository..*(..))")
    public void methodsToLogExecutionTime() {
    }

    @Around("methodsToLogExecutionTime()")
    public Object aroundAppMethod(ProceedingJoinPoint pjp) throws Throwable {
        var className = getClassName(pjp.getTarget());
        var methodName = pjp.getSignature().getName();
        var stopWatch = new StopWatch();
        stopWatch.start();
        try {
            log.info("Executing {}#{}", className, methodName);
            return pjp.proceed();
        } finally {
            stopWatch.stop();
            log.info("Exiting {}#{} executed in: {}ms", className, methodName, stopWatch.getTotalTimeMillis());
        }
    }


    private String getClassName(Object target) {
        return FrpUtils.abbreviate(getClassNameInner(target));
    }

    private String getClassNameInner(Object target) {
        if (AopUtils.isJdkDynamicProxy(target)) {
            var interfaces = AopProxyUtils.proxiedUserInterfaces(target);
            if (interfaces.length > 0) {
                return interfaces[0].getName();
            }
        }
        return target.getClass().getName();
    }
}
