package com.interviewlab.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.message.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Injected at the service layer — never hardcoded in the Message entity.
 */
@ConfigurationProperties(prefix = "app.message")
public record MessageProperties(boolean defaultVoiceUsed) {}
