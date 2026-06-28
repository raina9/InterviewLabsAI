package com.interviewlab.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory for AIProvider implementations (Factory pattern).
 * All providers are injected — switch must be exhaustive across all AiProvider enum values.
 * Active provider is selected via getDefaultProvider() which reads AiProperties.
 * OLLAMA: active. GEMINI/CLAUDE/OPENAI: parked (stubs throw UnsupportedOperationException).
 */
@RequiredArgsConstructor
@Component
public class AIProviderFactory {

    private final OllamaProvider  ollamaProvider;
    private final GeminiProvider  geminiProvider;
    private final ClaudeProvider  claudeProvider;
    private final OpenAIProvider  openAIProvider;
    private final AiProperties    aiProperties;

    public AiProviderStrategy getProvider(AiProvider type) {
        return switch (type) {
            case OLLAMA -> ollamaProvider;
            case GEMINI -> geminiProvider;
            case CLAUDE -> claudeProvider;
            case OPENAI -> openAIProvider;
        };
    }

    public AiProviderStrategy getDefaultProvider() {
        return getProvider(aiProperties.defaultProvider());
    }
}
