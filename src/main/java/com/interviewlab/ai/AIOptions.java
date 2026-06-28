package com.interviewlab.ai;

/**
 * Immutable call-level options passed to AIProvider per invocation.
 * Static factory methods use values that match the env-var defaults in application.yml.
 * Agent services (Checkpoint 6) read AiProperties.options to create options at runtime.
 */
public record AIOptions(float temperature, int maxTokens, boolean jsonMode) {

    // Matches AI_DEFAULT_TEMPERATURE:0.7 and AI_DEFAULT_MAX_TOKENS:800
    public static AIOptions defaults() {
        return new AIOptions(0.7f, 800, false);
    }

    // Matches AI_FEEDBACK_TEMPERATURE:0.3 and AI_FEEDBACK_MAX_TOKENS:500
    public static AIOptions forFeedback() {
        return new AIOptions(0.3f, 500, false);
    }

    // Matches AI_QUESTIONS_TEMPERATURE:0.7 and AI_QUESTIONS_MAX_TOKENS:1000
    public static AIOptions forQuestions() {
        return new AIOptions(0.7f, 1000, false);
    }
}
