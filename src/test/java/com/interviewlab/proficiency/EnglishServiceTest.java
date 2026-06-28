package com.interviewlab.proficiency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderException;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProviderStrategy;
import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnglishServiceTest {

    @Mock AIProviderFactory     aiProviderFactory;
    @Mock AiProviderStrategy    aiProvider;
    @Mock EnglishPromptBuilder  promptBuilder;
    @Mock EnglishProperties     englishProperties;
    @Mock ProficiencyRepository proficiencyRepository;
    @Mock ProficiencyProperties proficiencyProperties;
    @Spy  ObjectMapper          objectMapper = new ObjectMapper();

    @InjectMocks EnglishService englishService;

    private static final UUID USER_ID = UUID.randomUUID();

    private void stubProviderSetup() {
        when(englishProperties.temperature()).thenReturn(0.3f);
        when(englishProperties.maxTokens()).thenReturn(600);
        when(promptBuilder.buildAnalysisPrompt(anyString(), anyString())).thenReturn("test prompt");
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
    }

    private static final String VALID_JSON = """
        {
          "fluencyNote": "Clear and articulate delivery",
          "tenseFeedback": "No errors detected",
          "fillerWordsDetected": "um, uh",
          "vocabularyNote": "Good vocabulary range",
          "confidenceNote": "Sounds confident",
          "improvedVersion": "Improved transcript here",
          "fluencyScore": 8
        }
        """;

    // -------------------------------------------------------------------------
    // Scenario 1: valid transcript returns correctly parsed feedback
    // -------------------------------------------------------------------------

    @Test
    void analyze_validTranscript_returnsParsedFeedback() {
        stubProviderSetup();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_JSON);
        when(proficiencyRepository.findByUserIdAndTopic(USER_ID, "english")).thenReturn(Optional.empty());
        when(proficiencyProperties.defaultScore()).thenReturn(0.0);
        when(proficiencyProperties.defaultSessionsCount()).thenReturn(0);

        EnglishFeedback result = englishService.analyze("My spoken answer", "interview", USER_ID);

        assertThat(result.fluencyScore()).isEqualTo(8);
        assertThat(result.fluencyNote()).isEqualTo("Clear and articulate delivery");
        assertThat(result.tenseFeedback()).isEqualTo("No errors detected");
        assertThat(result.fillerWordsDetected()).isEqualTo("um, uh");
        assertThat(result.improvedVersion()).isEqualTo("Improved transcript here");

        verify(proficiencyRepository).save(any(Proficiency.class));
        verify(aiProvider).generateJson(anyString(), any(AIOptions.class));
    }

    // -------------------------------------------------------------------------
    // Scenario 2: AI returns garbage JSON → throws ENGLISH_ANALYSIS_FAILED
    // -------------------------------------------------------------------------

    @Test
    void analyze_malformedJson_throwsEnglishAnalysisFailed() {
        stubProviderSetup();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn("This is not valid JSON at all");

        assertThatThrownBy(() -> englishService.analyze("Some transcript", "interview", USER_ID))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> assertThat(((AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.ENGLISH_ANALYSIS_FAILED));

        verify(proficiencyRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Scenario 3: AI provider throws → exception propagates unchanged
    // -------------------------------------------------------------------------

    @Test
    void analyze_aiProviderThrows_propagatesException() {
        stubProviderSetup();
        AIProviderException aiError = new AIProviderException(
            ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, "AI unavailable"
        );
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenThrow(aiError);

        assertThatThrownBy(() -> englishService.analyze("Any transcript", "interview", USER_ID))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> assertThat(((AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));

        verify(proficiencyRepository, never()).save(any());
    }
}
