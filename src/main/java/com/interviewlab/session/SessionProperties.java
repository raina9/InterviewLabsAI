package com.interviewlab.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.session.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Injected at the service layer — never hardcoded in the Session entity.
 */
@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(SessionStatus defaultStatus) {}
