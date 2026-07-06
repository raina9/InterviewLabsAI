package com.interviewlab.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.rate-limit.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * dailyLimit — max requests per user per fixed 24h window (see RateLimitService, ADR-010).
 * mode — personal (relaxed, single user) | public (strict) | embedded (per-tenant).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(int dailyLimit, String mode) {}
