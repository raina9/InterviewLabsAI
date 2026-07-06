package com.interviewlab.ratelimit;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.sessionstore.InMemorySessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    // InMemorySessionStore/RateLimitProperties are constructed for real — no mocking
    // needed (same pattern as InterviewAgentTest/QuizServiceTest for record dependencies).
    private final InMemorySessionStore sessionStore = new InMemorySessionStore();

    RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(sessionStore, new RateLimitProperties(3, "personal"));
    }

    // -------------------------------------------------------------------------
    // Scenario 1: under the limit — checkAndIncrement never throws
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_underLimit_doesNotThrow() {
        UUID userId = UUID.randomUUID();

        rateLimitService.checkAndIncrement(userId);
        rateLimitService.checkAndIncrement(userId);
        rateLimitService.checkAndIncrement(userId);
    }

    // -------------------------------------------------------------------------
    // Scenario 2: exceeding the daily limit — throws RateLimitException
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_overLimit_throwsRateLimitException() {
        UUID userId = UUID.randomUUID();

        rateLimitService.checkAndIncrement(userId);
        rateLimitService.checkAndIncrement(userId);
        rateLimitService.checkAndIncrement(userId);

        assertThatThrownBy(() -> rateLimitService.checkAndIncrement(userId))
            .isInstanceOf(RateLimitException.class)
            .satisfies(ex -> assertThat(((RateLimitException) ex).errorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    // -------------------------------------------------------------------------
    // Scenario 3: separate users each get their own independent window
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_differentUsers_trackedIndependently() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        rateLimitService.checkAndIncrement(userA);
        rateLimitService.checkAndIncrement(userA);
        rateLimitService.checkAndIncrement(userA);

        // userB should still be well under its own limit
        rateLimitService.checkAndIncrement(userB);
    }
}
