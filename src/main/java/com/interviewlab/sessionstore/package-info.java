/**
 * Ephemeral keyed state abstraction — Swappable Backend Pattern.
 *
 * Active (default): InMemorySessionStore
 *   Condition: @ConditionalOnProperty(name="app.session.store", havingValue="memory", matchIfMissing=true)
 *   Config:    SESSION_STORE=memory (or unset)
 *   Behaviour: ConcurrentHashMap + lazy TTL check on read. Single-instance only.
 *   Cost:      zero
 *
 * Paid (cloud): RedisSessionStore
 *   Condition: @ConditionalOnProperty(name="app.session.store", havingValue="redis")
 *   Config:    SESSION_STORE=redis, REDIS_URL pointing at a reachable Redis instance
 *   Serialization: GenericJackson2JsonRedisSerializer only (wired in RedisConfig) —
 *                  never JDK serialization, which breaks on Java records.
 *   Activation steps:
 *     1. spring-data-redis is already an active dependency (pom.xml) — no uncomment needed.
 *     2. Set SESSION_STORE=redis and REDIS_URL in environment.
 *     3. DeploymentModeValidator refuses to start in DEPLOYMENT_MODE=production without REDIS_URL.
 *
 * AWS equivalent: ElastiCache for Redis — REDIS_URL points at the ElastiCache endpoint,
 * zero code change (see pom.xml AWS ElastiCache comment block).
 *
 * Consumers: QuizService, DrillService, CodeChallengeService (session state),
 * RateLimitService (fixed-window counters). All inject SessionStore, never the
 * concrete class — the bean swap is invisible to callers.
 */
package com.interviewlab.sessionstore;
