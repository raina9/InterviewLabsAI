package com.interviewlab.curriculum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.ai.AiProvider;
import com.interviewlab.ai.AiProviderStrategy;
import com.interviewlab.assessment.AssessmentException;
import com.interviewlab.assessment.AssessmentReport;
import com.interviewlab.assessment.AssessmentService;
import com.interviewlab.assessment.TopicScore;
import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceTest {

    @Mock AssessmentService       assessmentService;
    @Mock CurriculumPromptBuilder  promptBuilder;
    @Mock AIProviderFactory       aiProviderFactory;
    @Mock AiProviderStrategy      aiProvider;
    @Spy  ObjectMapper            objectMapper = new ObjectMapper();

    // AiProperties is a record — Mockito @InjectMocks cannot supply it, so it is
    // constructed for real (same pattern as InterviewAgentTest/MentorAgentTest).
    private final AiProperties aiProperties = new AiProperties(
        AiProvider.OLLAMA,
        120,
        new AiProperties.GeminiConfig("gemini-flash-lite-latest", "http://test", "key"),
        new AiProperties.OptionsConfig(0.7f, 800, 0.3f, 500, 0.7f, 1000),
        new AiProperties.QuizOptions(0.7f, 1000),
        new AiProperties.CodeOptions(0.7f, 1000, 0.3f, 800),
        new AiProperties.CurriculumOptions(0.5f, 1000),
        new AiProperties.DrillOptions(0.7f, 800, 0.3f, 500, 0.5f, 700)
    );

    CurriculumService curriculumService;

    @BeforeEach
    void setUp() {
        curriculumService = new CurriculumService(
            assessmentService, promptBuilder, aiProviderFactory, objectMapper, aiProperties
        );
    }

    private static final UUID USER_ID = UUID.randomUUID();

    private static final String VALID_JSON = """
        {
          "items": [
            {
              "topic": "Docker",
              "priority": "HIGH",
              "whyThisMatters": "Container orchestration is essential for production deployments.",
              "estimatedDays": "5",
              "keyConceptsToCover": ["Dockerfile", "docker-compose", "multi-stage builds"]
            }
          ],
          "estimatedWeeks": "4",
          "focus": "Container and orchestration fundamentals"
        }
        """;

    private AssessmentReport sampleReport() {
        return new AssessmentReport(
            List.of(new TopicScore("Docker", 3, "Beginner", "Start with fundamentals.")),
            "Beginner",
            List.of("Docker"),
            List.of()
        );
    }

    // -------------------------------------------------------------------------
    // Scenario 1: valid AI response → parsed CurriculumPlan returned
    // -------------------------------------------------------------------------

    @Test
    void generatePlan_validAiResponse_returnsParsedPlan() {
        when(assessmentService.generateReport(USER_ID)).thenReturn(sampleReport());
        when(promptBuilder.buildCurriculumPrompt(any(AssessmentReport.class))).thenReturn("test prompt");
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_JSON);

        CurriculumPlan plan = curriculumService.generatePlan(USER_ID);

        assertThat(plan.items()).hasSize(1);
        assertThat(plan.items().get(0).topic()).isEqualTo("Docker");
        assertThat(plan.items().get(0).priority()).isEqualTo("HIGH");
        assertThat(plan.estimatedWeeks()).isEqualTo("4");
        assertThat(plan.focus()).isEqualTo("Container and orchestration fundamentals");
    }

    // -------------------------------------------------------------------------
    // Scenario 2: AI returns garbage JSON → throws CURRICULUM_GENERATION_FAILED
    // -------------------------------------------------------------------------

    @Test
    void generatePlan_malformedAiJson_throwsCurriculumException() {
        when(assessmentService.generateReport(USER_ID)).thenReturn(sampleReport());
        when(promptBuilder.buildCurriculumPrompt(any(AssessmentReport.class))).thenReturn("test prompt");
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn("not json at all");

        assertThatThrownBy(() -> curriculumService.generatePlan(USER_ID))
            .isInstanceOf(CurriculumException.class)
            .satisfies(ex -> {
                CurriculumException ce = (CurriculumException) ex;
                assertThat(ce.errorCode()).isEqualTo(ErrorCode.CURRICULUM_GENERATION_FAILED);
                assertThat(ce.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            });
    }

    // -------------------------------------------------------------------------
    // Scenario 3: assessment not found → AssessmentException propagates
    // -------------------------------------------------------------------------

    @Test
    void generatePlan_assessmentNotFound_propagatesAssessmentException() {
        AssessmentException upstream = new AssessmentException(
            ErrorCode.ASSESSMENT_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            "No assessment data found."
        );
        when(assessmentService.generateReport(USER_ID)).thenThrow(upstream);

        assertThatThrownBy(() -> curriculumService.generatePlan(USER_ID))
            .isInstanceOf(AssessmentException.class)
            .satisfies(ex -> assertThat(((AssessmentException) ex).errorCode())
                .isEqualTo(ErrorCode.ASSESSMENT_NOT_FOUND));

        verify(aiProviderFactory, never()).getDefaultProvider();
    }
}
