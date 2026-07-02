package com.interviewlab.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.ai.AiProvider;
import com.interviewlab.ai.AiProviderStrategy;
import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeChallengeServiceTest {

    @Mock AIProviderFactory  aiProviderFactory;
    @Mock AiProviderStrategy aiProvider;
    @Mock Judge0Properties   judge0Properties;
    @Mock RestClient.Builder restClientBuilder;
    @Spy  ObjectMapper       objectMapper = new ObjectMapper();

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

    CodeChallengeService codeChallengeService;

    @BeforeEach
    void setUp() {
        codeChallengeService = new CodeChallengeService(
            aiProviderFactory, judge0Properties, objectMapper, restClientBuilder, aiProperties
        );
    }

    private static final String VALID_CHALLENGE_JSON = """
        {
          "title": "Two Sum",
          "description": "Find two numbers in an array that add up to a target.",
          "starterCode": {
            "java": "// Java starter",
            "python": "# Python starter",
            "javascript": "// JS starter"
          },
          "testCases": ["Input: [2,7,11,15], target=9 Expected: [0,1]"],
          "constraints": ["2 <= nums.length <= 10^4", "All integers are distinct"]
        }
        """;

    private static final String VALID_REVIEW_JSON = """
        {
          "passed": true,
          "feedback": "Correct approach using HashMap.",
          "refinedCode": "// refined",
          "explanation": "Two-pointer solution runs in O(n)."
        }
        """;

    // -------------------------------------------------------------------------
    // Scenario 1: generate challenge — valid AI response → CodeChallenge returned
    // -------------------------------------------------------------------------

    @Test
    void generateChallenge_validAiResponse_returnsChallenge() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_CHALLENGE_JSON);

        CodeChallenge challenge = codeChallengeService.generateChallenge(
            new CodeChallengeRequest("Arrays", "medium")
        );

        assertThat(challenge.id()).isNotNull();
        assertThat(challenge.title()).isEqualTo("Two Sum");
        assertThat(challenge.description()).contains("target");
        assertThat(challenge.testCases()).isNotEmpty();
        assertThat(challenge.starterCode()).containsKey("java");
    }

    // -------------------------------------------------------------------------
    // Scenario 2: generate challenge — malformed JSON → CODE_CHALLENGE_GENERATION_FAILED
    // -------------------------------------------------------------------------

    @Test
    void generateChallenge_malformedJson_throwsGenerationFailed() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn("not json");

        assertThatThrownBy(() -> codeChallengeService.generateChallenge(
                new CodeChallengeRequest("Arrays", "easy")
        ))
            .isInstanceOf(CodeChallengeException.class)
            .satisfies(ex -> {
                CodeChallengeException ce = (CodeChallengeException) ex;
                assertThat(ce.errorCode()).isEqualTo(ErrorCode.CODE_CHALLENGE_GENERATION_FAILED);
                assertThat(ce.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            });
    }

    // -------------------------------------------------------------------------
    // Scenario 3: submit to unknown challenge → CODE_CHALLENGE_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void submitSolution_unknownChallenge_throwsNotFound() {
        assertThatThrownBy(() -> codeChallengeService.submitSolution(
            new CodeSubmitRequest(UUID.randomUUID(), "// code", "java")
        ))
            .isInstanceOf(CodeChallengeException.class)
            .satisfies(ex -> assertThat(((CodeChallengeException) ex).errorCode())
                .isEqualTo(ErrorCode.CODE_CHALLENGE_NOT_FOUND));

        verify(aiProviderFactory, never()).getDefaultProvider();
    }

    // -------------------------------------------------------------------------
    // Scenario 4: submit with Judge0 not configured → AI code review as fallback
    // -------------------------------------------------------------------------

    @Test
    void submitSolution_judge0NotConfigured_usesAiReview() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn(VALID_CHALLENGE_JSON)  // first call: challenge generation
            .thenReturn(VALID_REVIEW_JSON);    // second call: AI review
        when(judge0Properties.isConfigured()).thenReturn(false);

        CodeChallenge challenge = codeChallengeService.generateChallenge(
            new CodeChallengeRequest("Arrays", "easy")
        );
        CodeSubmitResponse response = codeChallengeService.submitSolution(
            new CodeSubmitRequest(challenge.id(), "HashMap<Integer,Integer> map = new HashMap<>();", "java")
        );

        assertThat(response.passed()).isTrue();
        assertThat(response.feedback()).isEqualTo("Correct approach using HashMap.");
        // Judge0 was NOT called
        verify(restClientBuilder, never()).build();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: hint for known challenge → non-blank hint returned
    // -------------------------------------------------------------------------

    @Test
    void getHint_knownChallenge_returnsHint() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_CHALLENGE_JSON);
        when(aiProvider.generate(anyString(), any(AIOptions.class))).thenReturn("Think about using a HashMap for O(1) lookup.");

        CodeChallenge challenge = codeChallengeService.generateChallenge(
            new CodeChallengeRequest("Arrays", "easy")
        );
        String hint = codeChallengeService.getHint(challenge.id());

        assertThat(hint).isNotBlank();
        assertThat(hint).contains("HashMap");
    }

    // -------------------------------------------------------------------------
    // Scenario 6: hint for unknown challenge → CODE_CHALLENGE_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void getHint_unknownChallenge_throwsNotFound() {
        assertThatThrownBy(() -> codeChallengeService.getHint(UUID.randomUUID()))
            .isInstanceOf(CodeChallengeException.class)
            .satisfies(ex -> assertThat(((CodeChallengeException) ex).errorCode())
                .isEqualTo(ErrorCode.CODE_CHALLENGE_NOT_FOUND));
    }
}
