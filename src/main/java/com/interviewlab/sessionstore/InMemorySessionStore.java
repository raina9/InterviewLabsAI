package com.interviewlab.sessionstore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process session store — active by default (SESSION_STORE=memory or unset).
 * Single-instance only: state is lost on restart and not shared across pods.
 *
 * Swap to RedisSessionStore by setting SESSION_STORE=redis. See pom.xml for the
 * spring-data-redis activation and sessionstore/package-info.java for the full path.
 *
 * Expired entries are removed lazily — on the next get()/increment() call for that key —
 * rather than via a background sweep, matching the zero-infra cost of this implementation.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.session.store", havingValue = "memory", matchIfMissing = true)
public class InMemorySessionStore implements SessionStore {

    private record Entry(Object value, long expiresAtEpochMillis) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtEpochMillis;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public <T> void put(String key, T value, long ttlHours) {
        store.put(key, new Entry(value, expiryFor(ttlHours)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            store.remove(key);
            return null;
        }
        return type.cast(entry.value());
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public long increment(String key, long ttlHours) {
        Entry updated = store.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new Entry(1L, expiryFor(ttlHours));
            }
            return new Entry((Long) existing.value() + 1, existing.expiresAtEpochMillis());
        });
        return (Long) updated.value();
    }

    private long expiryFor(long ttlHours) {
        return System.currentTimeMillis() + Duration.ofHours(ttlHours).toMillis();
    }
}
