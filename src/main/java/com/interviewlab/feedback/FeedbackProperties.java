package com.interviewlab.feedback;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.feedback.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Injected at the service layer — never hardcoded in the SystemFeedback entity.
 */
@ConfigurationProperties(prefix = "app.feedback")
public record FeedbackProperties(boolean defaultApplied) {}
