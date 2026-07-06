package com.interviewlab.sessionstore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed session store — multi-instance safe. Activate with SESSION_STORE=redis.
 *
 * Uses GenericJackson2JsonRedisSerializer only (wired in RedisConfig) — never JDK
 * serialization, which breaks on Java records. Every key is written with an explicit
 * TTL; there is no unbounded key in this store.
 *
 * AWS equivalent: ElastiCache for Redis — point REDIS_URL at the ElastiCache endpoint,
 * zero code change (see pom.xml AWS ElastiCache comment block).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.session.store", havingValue = "redis")
public class RedisSessionStore implements SessionStore {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> void put(String key, T value, long ttlHours) {
        redisTemplate.opsForValue().set(key, value, Duration.ofHours(ttlHours));
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : type.cast(value);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public long increment(String key, long ttlHours) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofHours(ttlHours));
        }
        return count == null ? 0L : count;
    }
}
