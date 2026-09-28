package com.blog.common.ratelimit;

import java.lang.annotation.*;

/**
 * 限流注解，基于Redis INCR + EXPIRE
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流Key标识（会拼接IP作为完整key）
     */
    String key();

    /**
     * 时间窗口内最大请求次数
     */
    int limit();

    /**
     * 时间窗口，秒
     */
    int window();
}
