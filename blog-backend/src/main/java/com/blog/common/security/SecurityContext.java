package com.blog.common.security;

import lombok.Data;

import java.util.Set;

/**
 * 请求上下文，通过ThreadLocal在请求生命周期内传递登录用户信息
 */
@Data
public class SecurityContext {
    private static final ThreadLocal<SecurityContext> HOLDER = new ThreadLocal<>();

    private Long userId;
    private String username;
    private Set<String> perms;

    public static void set(SecurityContext ctx) {
        HOLDER.set(ctx);
    }

    public static SecurityContext get() {
        return HOLDER.get();
    }

    public static Long getCurrentUserId() {
        SecurityContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getUserId();
    }

    public static Set<String> getCurrentPerms() {
        SecurityContext ctx = HOLDER.get();
        return ctx == null ? null : ctx.getPerms();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 判断当前用户是否拥有指定权限码
     */
    public static boolean hasPerm(String permCode) {
        Set<String> perms = getCurrentPerms();
        return perms != null && perms.contains(permCode);
    }
}
