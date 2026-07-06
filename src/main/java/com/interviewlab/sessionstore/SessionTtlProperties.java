package com.interviewlab.sessionstore;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.session.ttl-hours.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Controls how long quiz/drill/code challenge state survives in SessionStore
 * before lazy expiry — independent of the store implementation (memory or Redis).
 */
@ConfigurationProperties(prefix = "app.session.ttl-hours")
public record SessionTtlProperties(
    long quiz,
    long drill,
    long code
) {}
