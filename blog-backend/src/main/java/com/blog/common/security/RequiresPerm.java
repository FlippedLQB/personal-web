package com.blog.common.security;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * value: 必须全部拥有的权限码（AND关系）
 * anyOf: 拥有其中任一权限码即可（OR关系）
 * value 和 anyOf 同时存在时，两组条件都需满足
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPerm {

    /**
     * 必须全部拥有的权限码
     */
    String[] value() default {};

    /**
     * 拥有任一即可的权限码
     */
    String[] anyOf() default {};
}
