package com.interviewlab.ai;

/**
 * AI provider interface — Strategy pattern.
 * Named AiProviderStrategy to avoid file-system collision with AiProvider enum on case-insensitive OS.
 * Exhaustive coverage is enforced by the AiProvider enum switch in AIProviderFactory —
 * sealed permits on the interface itself would only prevent testability (Mockito can't proxy sealed types).
 * Implementations: GeminiProvider (active), ClaudeProvider (parked), OpenAIProvider (parked).
 */
public interface AiProviderStrategy {

    String generate(String prompt, AIOptions options);

    String generateJson(String prompt, AIOptions options);

    AiProvider providerName();
}
