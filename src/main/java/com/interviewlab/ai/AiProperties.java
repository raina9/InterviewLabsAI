package com.interviewlab.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.ai.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 *
 * OptionsConfig — general-purpose agent call options (interview Q&A, mentor feedback, defaults).
 * QuizOptions / CodeOptions / CurriculumOptions / DrillOptions — domain-specific AI call tuning.
 * These domain configs live under app.ai.* (not app.ai.options.*) because they have distinct
 * token budgets that differ significantly from general agent options.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    AiProvider    defaultProvider,
    int           requestTimeoutSeconds,
    GeminiConfig  gemini,
    OptionsConfig options,
    QuizOptions   quiz,
    CodeOptions   code,
    CurriculumOptions curriculum,
    DrillOptions  drill
) {
    public record GeminiConfig(
        String model,
        String apiUrl,
        String apiKey
    ) {}

    /** General agent options — interview question generation and mentor feedback. */
    public record OptionsConfig(
        float defaultTemperature,
        int   defaultMaxTokens,
        float feedbackTemperature,
        int   feedbackMaxTokens,
        float questionsTemperature,
        int   questionsMaxTokens
    ) {}

    /** MCQ quiz generation — higher token budget for multi-question batch generation. */
    public record QuizOptions(
        float temperature,
        int   maxTokens
    ) {}

    /** Code challenge generation (more creative) and code review (precise). */
    public record CodeOptions(
        float generateTemperature,
        int   generateMaxTokens,
        float reviewTemperature,
        int   reviewMaxTokens
    ) {}

    /** Curriculum generation — moderate temperature for consistent priority ordering. */
    public record CurriculumOptions(
        float temperature,
        int   maxTokens
    ) {}

    /** Drill AI options — three call modes with different creativity/precision tradeoffs. */
    public record DrillOptions(
        float generateTemperature,
        int   generateMaxTokens,
        float evaluateTemperature,
        int   evaluateMaxTokens,
        float socraticTemperature,
        int   socraticMaxTokens
    ) {}
}
