package com.interviewlab.proficiency;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.english.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 */
@ConfigurationProperties(prefix = "app.english")
public record EnglishProperties(
    String systemPrompt,
    int    maxTokens,
    float  temperature
) {}
