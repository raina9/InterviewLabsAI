package com.interviewlab.ai;

import com.interviewlab.sessionstore.InMemorySessionStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIProviderFactoryTest {

    @Mock OllamaProvider  ollamaProvider;
    @Mock GeminiProvider  geminiProvider;
    @Mock ClaudeProvider  claudeProvider;
    @Mock OpenAIProvider  openAIProvider;

    // Generous limits — never blocks/exhausts in these routing-focused tests.
    private final AiQueueProperties aiQueueProperties = new AiQueueProperties(100, 30, 100_000);
    private final AIRequestQueue    aiRequestQueue     = new AIRequestQueue(aiQueueProperties);
    private final AiBudgetGuard     aiBudgetGuard      =
        new AiBudgetGuard(new InMemorySessionStore(), aiQueueProperties, new SimpleMeterRegistry());

    AIProviderFactory factory;

    AiProperties aiPropertiesGemini = new AiProperties(
        AiProvider.GEMINI,
        120,
        new AiProperties.GeminiConfig("model", "http://test", "key"),
        new AiProperties.OptionsConfig(0.7f, 800, 0.3f, 500, 0.7f, 1000),
        new AiProperties.QuizOptions(0.7f, 1000),
        new AiProperties.CodeOptions(0.7f, 1000, 0.3f, 800),
        new AiProperties.CurriculumOptions(0.5f, 1000),
        new AiProperties.DrillOptions(0.7f, 800, 0.3f, 500, 0.5f, 700)
    );

    AiProperties aiPropertiesClaude = new AiProperties(
        AiProvider.CLAUDE,
        120,
        new AiProperties.GeminiConfig("model", "http://test", "key"),
        new AiProperties.OptionsConfig(0.7f, 800, 0.3f, 500, 0.7f, 1000),
        new AiProperties.QuizOptions(0.7f, 1000),
        new AiProperties.CodeOptions(0.7f, 1000, 0.3f, 800),
        new AiProperties.CurriculumOptions(0.5f, 1000),
        new AiProperties.DrillOptions(0.7f, 800, 0.3f, 500, 0.5f, 700)
    );

    @BeforeEach
    void setUp() {
        factory = new AIProviderFactory(
            ollamaProvider, geminiProvider, Optional.of(claudeProvider), Optional.of(openAIProvider),
            aiPropertiesGemini, aiRequestQueue, aiBudgetGuard
        );
    }

    // getProvider()/getDefaultProvider() now wrap the raw provider in QueuedAiProviderStrategy
    // (single choke point for the request queue + budget guard) — assertions unwrap via the
    // package-private delegate() accessor to verify routing without changing test intent.

    @Test
    void getProvider_gemini_returnsGeminiProvider() {
        assertThat(((QueuedAiProviderStrategy) factory.getProvider(AiProvider.GEMINI)).delegate())
            .isSameAs(geminiProvider);
    }

    @Test
    void getProvider_claude_returnsClaudeProvider() {
        assertThat(((QueuedAiProviderStrategy) factory.getProvider(AiProvider.CLAUDE)).delegate())
            .isSameAs(claudeProvider);
    }

    @Test
    void getProvider_openai_returnsOpenAIProvider() {
        assertThat(((QueuedAiProviderStrategy) factory.getProvider(AiProvider.OPENAI)).delegate())
            .isSameAs(openAIProvider);
    }

    @Test
    void getDefaultProvider_configuredGemini_returnsGeminiProvider() {
        assertThat(((QueuedAiProviderStrategy) factory.getDefaultProvider()).delegate())
            .isSameAs(geminiProvider);
    }

    @Test
    void getDefaultProvider_configuredClaude_returnsClaudeProvider() {
        AIProviderFactory claudeFactory = new AIProviderFactory(
            ollamaProvider, geminiProvider, Optional.of(claudeProvider), Optional.of(openAIProvider),
            aiPropertiesClaude, aiRequestQueue, aiBudgetGuard
        );
        assertThat(((QueuedAiProviderStrategy) claudeFactory.getDefaultProvider()).delegate())
            .isSameAs(claudeProvider);
    }
}
