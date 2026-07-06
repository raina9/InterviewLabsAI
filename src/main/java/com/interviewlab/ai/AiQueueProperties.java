package com.interviewlab.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.ai.queue.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * maxConcurrent/timeoutSeconds — AIRequestQueue (semaphore choke point on every provider call).
 * dailyGlobalLimit — AiBudgetGuard (global daily call cap across all users, all providers).
 */
@ConfigurationProperties(prefix = "app.ai.queue")
public record AiQueueProperties(
    int  maxConcurrent,
    long timeoutSeconds,
    long dailyGlobalLimit
) {}
