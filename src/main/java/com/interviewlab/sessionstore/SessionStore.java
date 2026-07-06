package com.interviewlab.sessionstore;

/**
 * Ephemeral keyed state abstraction — Swappable Backend Pattern.
 *
 * Free implementation: InMemorySessionStore (ConcurrentHashMap, single-instance only).
 * Paid implementation: RedisSessionStore (Redis, multi-instance safe) — see pom.xml and package-info.java.
 *
 * Switch via SESSION_STORE env var — no code change required.
 * Every key carries a TTL; no key is stored without an expiry.
 */
public interface SessionStore {

    /**
     * Store a value under the given key, expiring after ttlHours.
     */
    <T> void put(String key, T value, long ttlHours);

    /**
     * Retrieve a previously stored value, or null if absent or expired.
     */
    <T> T get(String key, Class<T> type);

    /**
     * Remove the key. No-op if the key does not exist.
     */
    void delete(String key);

    /**
     * Atomically increment the counter at key and return the new count.
     * TTL is applied only when the key is first created — the window (and its
     * expiry) does not reset on subsequent increments. Used for fixed-window
     * rate limiting (see RateLimitService).
     */
    long increment(String key, long ttlHours);
}
