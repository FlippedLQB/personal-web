package com.blog.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return null;
        }
        return clazz.cast(val);
    }

    public String get(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        return val == null ? null : val.toString();
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 自增并设置过期时间（首次自增时设置TTL）
     */
    public long increment(String key, long delta, int expireSeconds) {
        Long count = redisTemplate.opsForValue().increment(key, delta);
        if (count != null && count == 1) {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 0 : count;
    }

    /**
     * 原子取值并删除
     */
    public Long getAndDelete(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return null;
        }
        redisTemplate.delete(key);
        return Long.parseLong(val.toString());
    }

    public void expire(String key, long seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    /**
     * 获取key的剩余过期时间（秒），不存在返回-2，无过期时间返回-1
     */
    public long getExpire(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null ? -2 : ttl;
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}
