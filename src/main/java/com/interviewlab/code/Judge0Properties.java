package com.interviewlab.code;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.judge0.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * When url or apiKey is blank, Judge0 execution is disabled and AI code review is used instead.
 */
@ConfigurationProperties(prefix = "app.judge0")
public record Judge0Properties(String url, String apiKey) {

    public boolean isConfigured() {
        return url != null && !url.isBlank() && apiKey != null && !apiKey.isBlank();
    }
}
