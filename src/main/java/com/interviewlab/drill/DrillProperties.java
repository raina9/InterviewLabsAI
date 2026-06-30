package com.interviewlab.drill;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.drill.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Controls session length for each drill mode — not AI tuning (see AiProperties.DrillOptions).
 */
@ConfigurationProperties(prefix = "app.drill")
public record DrillProperties(
    int rapidQuestionLimit,
    int deepTurnLimit
) {}
