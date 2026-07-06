package com.interviewlab.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Factory for AIProvider implementations (Factory pattern).
 *
 * Ollama and Gemini are always instantiated (active providers, no conditional).
 * Claude and OpenAI are guarded by @ConditionalOnProperty — they are only registered
 * as beans when app.ai.provider matches their respective value. The factory receives
 * them as Optional<T> so startup never fails when a parked provider is not configured.
 *
 * Switch the active provider via AI_PROVIDER env var — no code change required.
 */
@Slf4j
@Component
public class AIProviderFactory {

    private final OllamaProvider           ollamaProvider;
    private final GeminiProvider           geminiProvider;
    private final Optional<ClaudeProvider> claudeProvider;
    private final Optional<OpenAIProvider> openAIProvider;
    private final AiProperties             aiProperties;
    private final AIRequestQueue           aiRequestQueue;
    private final AiBudgetGuard            aiBudgetGuard;

    public AIProviderFactory(
            OllamaProvider ollamaProvider,
            GeminiProvider geminiProvider,
            Optional<ClaudeProvider> claudeProvider,
            Optional<OpenAIProvider> openAIProvider,
            AiProperties aiProperties,
            AIRequestQueue aiRequestQueue,
            AiBudgetGuard aiBudgetGuard) {
        this.ollamaProvider = ollamaProvider;
        this.geminiProvider = geminiProvider;
        this.claudeProvider = claudeProvider;
        this.openAIProvider = openAIProvider;
        this.aiProperties   = aiProperties;
        this.aiRequestQueue = aiRequestQueue;
        this.aiBudgetGuard  = aiBudgetGuard;
        log.info("AIProviderFactory initialised — default={} claude={} openai={}",
            aiProperties.defaultProvider(),
            claudeProvider.isPresent() ? "active" : "parked",
            openAIProvider.isPresent() ? "active" : "parked");
    }

    /**
     * Every provider returned here is wrapped in QueuedAiProviderStrategy — the single
     * choke point through which all AI calls pass AIRequestQueue and AiBudgetGuard.
     * Callers still just call generate()/generateJson() as normal; the wrapping is invisible.
     */
    public AiProviderStrategy getProvider(AiProvider type) {
        AiProviderStrategy delegate = switch (type) {
            case OLLAMA -> ollamaProvider;
            case GEMINI -> geminiProvider;
            case CLAUDE -> claudeProvider.orElseThrow(() -> new IllegalStateException(
                "Claude provider is not active — set AI_PROVIDER=claude and CLAUDE_API_KEY to enable it"));
            case OPENAI -> openAIProvider.orElseThrow(() -> new IllegalStateException(
                "OpenAI provider is not active — set AI_PROVIDER=openai and OPENAI_API_KEY to enable it"));
        };
        return new QueuedAiProviderStrategy(delegate, aiRequestQueue, aiBudgetGuard);
    }

    public AiProviderStrategy getDefaultProvider() {
        return getProvider(aiProperties.defaultProvider());
    }
}
