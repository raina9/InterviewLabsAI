package com.interviewlab.ai;

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
            ollamaProvider, geminiProvider, Optional.of(claudeProvider), Optional.of(openAIProvider), aiPropertiesGemini
        );
    }

    @Test
    void getProvider_gemini_returnsGeminiProvider() {
        assertThat(factory.getProvider(AiProvider.GEMINI)).isSameAs(geminiProvider);
    }

    @Test
    void getProvider_claude_returnsClaudeProvider() {
        assertThat(factory.getProvider(AiProvider.CLAUDE)).isSameAs(claudeProvider);
    }

    @Test
    void getProvider_openai_returnsOpenAIProvider() {
        assertThat(factory.getProvider(AiProvider.OPENAI)).isSameAs(openAIProvider);
    }

    @Test
    void getDefaultProvider_configuredGemini_returnsGeminiProvider() {
        assertThat(factory.getDefaultProvider()).isSameAs(geminiProvider);
    }

    @Test
    void getDefaultProvider_configuredClaude_returnsClaudeProvider() {
        AIProviderFactory claudeFactory = new AIProviderFactory(
            ollamaProvider, geminiProvider, Optional.of(claudeProvider), Optional.of(openAIProvider), aiPropertiesClaude
        );
        assertThat(claudeFactory.getDefaultProvider()).isSameAs(claudeProvider);
    }
}
