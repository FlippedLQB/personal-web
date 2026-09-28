package com.blog.common.security;

import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Aspect
@Component
public class RequiresPermAspect {

    @Around("@annotation(com.blog.common.security.RequiresPerm) || @within(com.blog.common.security.RequiresPerm)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresPerm annotation = signature.getMethod().getAnnotation(RequiresPerm.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequiresPerm.class);
        }
        if (annotation == null) {
            return joinPoint.proceed();
        }

        Set<String> userPerms = SecurityContext.getCurrentPerms();
        if (userPerms == null) {
            throw new BizException(BizCodeEnum.UNAUTHORIZED);
        }

        // 校验 value（必须全部拥有）
        if (annotation.value().length > 0) {
            for (String perm : annotation.value()) {
                if (!userPerms.contains(perm)) {
                    log.warn("权限不足, 需要: {}, 用户权限: {}", perm, userPerms);
                    throw new BizException(BizCodeEnum.FORBIDDEN);
                }
            }
        }

        // 校验 anyOf（任一拥有即可）
        if (annotation.anyOf().length > 0) {
            boolean hasAny = Arrays.stream(annotation.anyOf()).anyMatch(userPerms::contains);
            if (!hasAny) {
                log.warn("权限不足, 需要任一: {}, 用户权限: {}", Arrays.toString(annotation.anyOf()), userPerms);
                throw new BizException(BizCodeEnum.FORBIDDEN);
            }
        }

        return joinPoint.proceed();
    }
}
