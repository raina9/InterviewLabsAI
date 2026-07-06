package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIRequestQueueTest {

    // -------------------------------------------------------------------------
    // Scenario 1: permit available — call executes and returns its result
    // -------------------------------------------------------------------------

    @Test
    void execute_permitAvailable_returnsCallResult() {
        AIRequestQueue queue = new AIRequestQueue(new AiQueueProperties(1, 5, 1000));

        String result = queue.execute(() -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    // -------------------------------------------------------------------------
    // Scenario 2: permit released after each call — a second call can reuse the same slot
    // -------------------------------------------------------------------------

    @Test
    void execute_afterRelease_permitReusable() {
        AIRequestQueue queue = new AIRequestQueue(new AiQueueProperties(1, 5, 1000));

        queue.execute(() -> "first");
        String second = queue.execute(() -> "second");

        assertThat(second).isEqualTo("second");
    }

    // -------------------------------------------------------------------------
    // Scenario 3: no permit available within timeout — AI_BUSY, 429, Retry-After carried
    // -------------------------------------------------------------------------

    @Test
    void execute_noPermitWithinTimeout_throwsAiBusy() throws InterruptedException {
        AIRequestQueue queue = new AIRequestQueue(new AiQueueProperties(1, 1, 1000));
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> queue.execute(() -> {
            holding.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        holder.start();
        assertThat(holding.await(2, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> queue.execute(() -> "should not run"))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> {
                AIProviderException aex = (AIProviderException) ex;
                assertThat(aex.errorCode()).isEqualTo(ErrorCode.AI_BUSY);
                assertThat(aex.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                assertThat(aex.retryAfterSeconds()).isEqualTo(1L);
            });

        release.countDown();
        holder.join(5000);
    }
}
