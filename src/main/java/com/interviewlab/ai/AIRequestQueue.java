package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Single choke point for every outbound AI provider call — a fair Semaphore bounding
 * concurrent in-flight requests to app.ai.queue.max-concurrent. A request that cannot
 * acquire a slot within app.ai.queue.timeout-seconds fails fast with AI_BUSY (429) rather
 * than piling up unbounded threads against a slow/local LLM. See ADR-011 for why a
 * Semaphore was chosen over reactive backpressure at the current scale.
 */
@Slf4j
@Component
public class AIRequestQueue {

    private final Semaphore semaphore;
    private final long      timeoutSeconds;

    public AIRequestQueue(AiQueueProperties aiQueueProperties) {
        // fair=true: FIFO ordering under contention — no request starves behind newer ones
        this.semaphore      = new Semaphore(aiQueueProperties.maxConcurrent(), true);
        this.timeoutSeconds = aiQueueProperties.timeoutSeconds();
    }

    public <T> T execute(Supplier<T> call) {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw busy();
        }

        if (!acquired) {
            log.warn("AI request queue full — no slot within {}s ({} permits total)",
                timeoutSeconds, semaphore.availablePermits());
            throw busy();
        }

        try {
            return call.get();
        } finally {
            semaphore.release();
        }
    }

    private AIProviderException busy() {
        return new AIProviderException(
            ErrorCode.AI_BUSY,
            HttpStatus.TOO_MANY_REQUESTS,
            "The AI service is at capacity — retry after " + timeoutSeconds + " seconds.",
            timeoutSeconds
        );
    }
}
