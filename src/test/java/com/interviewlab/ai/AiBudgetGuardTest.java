package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.sessionstore.InMemorySessionStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiBudgetGuardTest {

    private final InMemorySessionStore sessionStore = new InMemorySessionStore();

    // -------------------------------------------------------------------------
    // Scenario 1: under the daily global limit — never throws
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_underLimit_doesNotThrow() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 3));

        guard.checkAndIncrement();
        guard.checkAndIncrement();
        guard.checkAndIncrement();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: breaching the daily global limit — AI_BUDGET_EXHAUSTED, 503
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_overLimit_throwsBudgetExhausted() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 2));

        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThatThrownBy(guard::checkAndIncrement)
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> {
                AIProviderException aex = (AIProviderException) ex;
                assertThat(aex.errorCode()).isEqualTo(ErrorCode.AI_BUDGET_EXHAUSTED);
                assertThat(aex.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            });
    }

    // -------------------------------------------------------------------------
    // Scenario 3: counter is global — shared across calls regardless of "user"
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_sharedAcrossCallers_countsTowardSameLimit() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 1));

        guard.checkAndIncrement();

        assertThatThrownBy(guard::checkAndIncrement)
            .isInstanceOf(AIProviderException.class);
    }
}
