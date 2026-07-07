package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.sessionstore.InMemorySessionStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiBudgetGuardTest {

    private final InMemorySessionStore sessionStore  = new InMemorySessionStore();
    private final SimpleMeterRegistry  meterRegistry = new SimpleMeterRegistry();

    // -------------------------------------------------------------------------
    // Scenario 1: under the daily global limit — never throws
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_underLimit_doesNotThrow() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 3), meterRegistry);

        guard.checkAndIncrement();
        guard.checkAndIncrement();
        guard.checkAndIncrement();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: breaching the daily global limit — AI_BUDGET_EXHAUSTED, 503
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_overLimit_throwsBudgetExhausted() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 2), meterRegistry);

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
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 1), meterRegistry);

        guard.checkAndIncrement();

        assertThatThrownBy(guard::checkAndIncrement)
            .isInstanceOf(AIProviderException.class);
    }

    // -------------------------------------------------------------------------
    // Scenario 4: ai.calls.total increments once per successful call, never on rejection
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_underLimit_incrementsAiCallsTotalCounter() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 3), meterRegistry);

        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThat(meterRegistry.counter("ai.calls.total").count()).isEqualTo(2.0);
        assertThat(meterRegistry.counter("ai.budget.rejected").count()).isEqualTo(0.0);
    }

    // -------------------------------------------------------------------------
    // Scenario 5: ai.budget.rejected increments on the rejecting call only
    // -------------------------------------------------------------------------

    @Test
    void checkAndIncrement_overLimit_incrementsAiBudgetRejectedCounter() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 1), meterRegistry);

        guard.checkAndIncrement();
        assertThatThrownBy(guard::checkAndIncrement).isInstanceOf(AIProviderException.class);

        assertThat(meterRegistry.counter("ai.calls.total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("ai.budget.rejected").count()).isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // Scenario 6: todaysCallCount reads without incrementing
    // -------------------------------------------------------------------------

    @Test
    void todaysCallCount_returnsCurrentCountWithoutIncrementing() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 10), meterRegistry);

        guard.checkAndIncrement();
        guard.checkAndIncrement();

        assertThat(guard.todaysCallCount()).isEqualTo(2L);
        assertThat(guard.todaysCallCount()).isEqualTo(2L);
    }

    @Test
    void todaysCallCount_returnsZero_whenNoCallsYetToday() {
        AiBudgetGuard guard = new AiBudgetGuard(sessionStore, new AiQueueProperties(5, 30, 10), meterRegistry);

        assertThat(guard.todaysCallCount()).isEqualTo(0L);
    }
}
