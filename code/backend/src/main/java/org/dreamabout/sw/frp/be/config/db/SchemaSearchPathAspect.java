package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaSearchPathAspect {

    private static final ThreadLocal<String> currentPath = new ThreadLocal<>();

    private final JdbcTemplate jdbcTemplate;

    @Pointcut("execution(* org.dreamabout.sw.frp.be..repository..*(..))")
    public void anyRepositoryMethod() {
    }

    @Around("anyRepositoryMethod()")
    public Object aroundRepository(ProceedingJoinPoint pjp) throws Throwable {

        var tenantSchema = Optional.ofNullable(TenantContext.getCurrentTenant())
                .map(TenantIdentifier::getTenantId)
                .orElse(Constant.PUBLIC_SCHEMA);

        var expectedPath = tenantSchema.equals(Constant.PUBLIC_SCHEMA)
                ? Constant.PUBLIC_SCHEMA
                : tenantSchema + ", " + Constant.PUBLIC_SCHEMA;

        if (!expectedPath.equals(currentPath.get())) {
            log.info("Setting search path to {}", expectedPath);
            jdbcTemplate.execute("SET search_path TO " + expectedPath);
            currentPath.set(expectedPath);
        }

        return pjp.proceed();
    }
}
