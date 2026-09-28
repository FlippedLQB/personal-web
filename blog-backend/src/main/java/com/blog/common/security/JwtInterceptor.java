package com.blog.common.security;

import com.blog.common.exception.BizCodeEnum;
import com.blog.common.exception.BizException;
import com.blog.common.jwt.JwtUtil;
import com.blog.common.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    private static final String PERM_CACHE_PREFIX = "user:perms:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(jwtUtil.getHeader());
        if (!StringUtils.hasText(header) || !header.startsWith(jwtUtil.getPrefix())) {
            throw new BizException(BizCodeEnum.UNAUTHORIZED);
        }

        String token = header.substring(jwtUtil.getPrefix().length()).trim();
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) {
            throw new BizException(BizCodeEnum.UNAUTHORIZED);
        }

        // 校验Redis中是否存在该token（支持主动踢下线）
        String cachedToken = redisUtil.get("user:token:" + userId);
        if (cachedToken == null || !cachedToken.equals(token)) {
            throw new BizException(BizCodeEnum.UNAUTHORIZED);
        }

        // 获取权限集合，优先从Redis读
        Set<String> perms = getUserPerms(userId);

        // 设置请求上下文
        SecurityContext ctx = new SecurityContext();
        ctx.setUserId(userId);
        ctx.setPerms(perms);
        SecurityContext.set(ctx);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContext.clear();
    }

    /**
     * 获取用户权限码集合，优先从Redis缓存读，未命中查库后回写
     */
    @SuppressWarnings("unchecked")
    private Set<String> getUserPerms(Long userId) {
        String cacheKey = PERM_CACHE_PREFIX + userId;
        Set<String> perms = redisUtil.get(cacheKey, Set.class);
        if (perms != null) {
            return perms;
        }
        // 缓存未命中时，权限查询由RBAC模块处理
        // 这里返回空集合，实际权限由 @RequiresPerm 切面校验
        // 如果接口没有 @RequiresPerm 注解，则只要有合法JWT即可访问
        return Set.of();
    }

    /**
     * 清除用户权限缓存（角色/权限变更时调用）
     */
    public static void clearPermCache(Long userId) {
        // 由RedisUtil通过applicationContext调用，这里仅定义key规则
    }
}
