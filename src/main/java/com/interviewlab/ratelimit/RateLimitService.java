package com.interviewlab.ratelimit;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.sessionstore.SessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fixed-window rate limiting — one SessionStore.increment() call per request.
 * Deliberately not a sliding window: sliding window needs multiple Redis commands
 * per check (read + prune + write), which burns through Upstash's free-tier command
 * budget far faster than a single INCR per request. See ADR-010.
 *
 * Window = calendar day (UTC-independent, uses JVM default zone via LocalDate.now()).
 * A user's count resets at local midnight rather than 24h after their first request.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RateLimitService {

    private static final String KEY_PREFIX      = "ratelimit:";
    private static final long   WINDOW_TTL_HOURS = 24;

    private final SessionStore         sessionStore;
    private final RateLimitProperties  rateLimitProperties;

    /**
     * Increments the user's request count for the current window and throws
     * RateLimitException if the daily limit has been exceeded.
     */
    public void checkAndIncrement(UUID userId) {
        long count = sessionStore.increment(key(userId), WINDOW_TTL_HOURS);
        log.debug("Rate limit check: userId={} count={} limit={}", userId, count, rateLimitProperties.dailyLimit());
        if (count > rateLimitProperties.dailyLimit()) {
            throw new RateLimitException(
                ErrorCode.RATE_LIMIT_EXCEEDED,
                HttpStatus.TOO_MANY_REQUESTS,
                "Daily request limit of " + rateLimitProperties.dailyLimit() + " reached for this account. Try again after midnight."
            );
        }
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now();
    }
}
