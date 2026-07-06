package com.interviewlab.sessionstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSessionStoreTest {

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    RedisSessionStore store;

    @BeforeEach
    void setUp() {
        store = new RedisSessionStore(redisTemplate);
    }

    // -------------------------------------------------------------------------
    // Scenario 1: put — writes through opsForValue().set with the given TTL
    // -------------------------------------------------------------------------

    @Test
    void put_writesValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store.put("k1", "hello", 2);

        verify(valueOperations).set("k1", "hello", Duration.ofHours(2));
    }

    // -------------------------------------------------------------------------
    // Scenario 2: get — casts the deserialized value to the requested type
    // -------------------------------------------------------------------------

    @Test
    void get_existingKey_returnsTypedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("k1")).thenReturn("hello");

        String result = store.get("k1", String.class);

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void get_missingKey_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("missing")).thenReturn(null);

        assertThat(store.get("missing", String.class)).isNull();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: delete — delegates to redisTemplate.delete
    // -------------------------------------------------------------------------

    @Test
    void delete_delegatesToRedisTemplate() {
        store.delete("k1");

        verify(redisTemplate).delete("k1");
    }

    // -------------------------------------------------------------------------
    // Scenario 4: increment — first increment (count=1) sets TTL exactly once
    // -------------------------------------------------------------------------

    @Test
    void increment_firstCall_setsExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("counter")).thenReturn(1L);

        long count = store.increment("counter", 24);

        assertThat(count).isEqualTo(1L);
        verify(redisTemplate).expire("counter", Duration.ofHours(24));
    }

    // -------------------------------------------------------------------------
    // Scenario 5: increment — subsequent increments do not re-set TTL (fixed window)
    // -------------------------------------------------------------------------

    @Test
    void increment_subsequentCall_doesNotResetExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("counter")).thenReturn(2L);

        long count = store.increment("counter", 24);

        assertThat(count).isEqualTo(2L);
        verify(redisTemplate, never()).expire(any(), any(Duration.class));
    }
}
