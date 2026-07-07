package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.sessionstore.SessionStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Global daily kill switch — caps total AI provider calls across ALL users, ALL providers,
 * at app.ai.queue.daily-global-limit. Protects against a runaway bill from a bug (infinite
 * retry loop, misbehaving agent) or an abuse pattern that AIRequestQueue's per-request
 * concurrency cap alone would not catch, since concurrency stays low but call volume
 * over a day still climbs unbounded. See ADR-011.
 *
 * The counter increments only when this guard is actually invoked — callers must invoke
 * it AFTER acquiring an AIRequestQueue permit, so a request rejected as AI_BUSY never
 * consumes budget it didn't actually spend.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AiBudgetGuard {

    private static final String KEY_PREFIX = "ai:budget:";
    private static final long   TTL_HOURS  = 25;

    private final SessionStore       sessionStore;
    private final AiQueueProperties  aiQueueProperties;
    private final MeterRegistry      meterRegistry;

    public void checkAndIncrement() {
        long count = sessionStore.increment(key(), TTL_HOURS);
        if (count > aiQueueProperties.dailyGlobalLimit()) {
            log.error("[AI_BUDGET_ALERT] Daily AI call budget exhausted: count={} limit={}",
                count, aiQueueProperties.dailyGlobalLimit());
            meterRegistry.counter("ai.budget.rejected").increment();
            throw new AIProviderException(
                ErrorCode.AI_BUDGET_EXHAUSTED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Daily AI usage budget exhausted. Please try again tomorrow."
            );
        }
        meterRegistry.counter("ai.calls.total").increment();
    }

    /**
     * Today's AI call count without incrementing — read-only, used by admin stats.
     */
    public long todaysCallCount() {
        Long count = sessionStore.get(key(), Long.class);
        return count == null ? 0L : count;
    }

    private String key() {
        return KEY_PREFIX + LocalDate.now();
    }
}
