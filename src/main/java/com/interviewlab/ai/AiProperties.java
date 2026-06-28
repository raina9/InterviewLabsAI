package com.interviewlab.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.ai.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * GeminiConfig binds: app.ai.gemini.{model, api-url, api-key}
 * OptionsConfig binds: app.ai.options.{default-temperature, default-max-tokens, ...}
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    AiProvider    defaultProvider,
    GeminiConfig  gemini,
    OptionsConfig options
) {
    public record GeminiConfig(
        String model,
        String apiUrl,
        String apiKey
    ) {}

    public record OptionsConfig(
        float defaultTemperature,
        int   defaultMaxTokens,
        float feedbackTemperature,
        int   feedbackMaxTokens,
        float questionsTemperature,
        int   questionsMaxTokens
    ) {}
}
