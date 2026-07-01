package com.interviewlab.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.agent.tools.AgentContext;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.ai.AiProvider;
import com.interviewlab.ai.AiProviderStrategy;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.session.Session;
import com.interviewlab.session.InterviewType;
import com.interviewlab.session.SessionRepository;
import com.interviewlab.session.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MentorAgentTest {

    @Mock AgentToolChain          agentToolChain;
    @Mock MentorAgentPromptBuilder promptBuilder;
    @Mock AIProviderFactory       aiProviderFactory;
    @Mock SessionRepository       sessionRepository;
    @Mock AiProviderStrategy      aiProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MentorAgent mentorAgent;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();

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

    @BeforeEach
    void setUp() {
        mentorAgent = new MentorAgent(
            agentToolChain, promptBuilder, aiProviderFactory, sessionRepository, objectMapper, aiProperties
        );
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
    }

    @Test
    void analyze_validJsonResponse_parsesMentorFeedback() {
        Session session = new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(agentToolChain.execute(any(AgentContext.class))).thenReturn(Map.of());
        when(promptBuilder.buildFeedbackPrompt(any(), any(), any(), any())).thenReturn("Mentor prompt");

        String jsonResponse = """
            {
              "feedbackGood":    "Clear explanation of the concept",
              "feedbackImprove": "Add concrete examples from production",
              "refinedAnswer":   "In Java, memory management is handled by the JVM GC...",
              "modelAnswer":     "An ideal answer covers: heap vs stack, GC algorithms, tuning...",
              "psychologyNote":  "Structured thinking with production examples impresses interviewers",
              "score":           8
            }
            """;

        when(aiProvider.generateJson(eq("Mentor prompt"), any())).thenReturn(jsonResponse);

        MentorFeedback feedback = mentorAgent.analyze(SESSION_ID, MESSAGE_ID, "Explain Java memory model", "JVM manages memory using GC");

        assertThat(feedback.score()).isEqualTo(8);
        assertThat(feedback.feedbackGood()).isEqualTo("Clear explanation of the concept");
        assertThat(feedback.feedbackImprove()).isEqualTo("Add concrete examples from production");
        assertThat(feedback.refinedAnswer()).isNotBlank();
        assertThat(feedback.modelAnswer()).isNotBlank();
        assertThat(feedback.psychologyNote()).isNotBlank();
    }

    @Test
    void analyze_jsonWithPreamble_extractsJsonCorrectly() {
        Session session = new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(agentToolChain.execute(any(AgentContext.class))).thenReturn(Map.of());
        when(promptBuilder.buildFeedbackPrompt(any(), any(), any(), any())).thenReturn("Mentor prompt");

        // Gemini sometimes adds preamble before JSON
        String responseWithPreamble = "Here is my evaluation:\n" +
            "{\"feedbackGood\": \"Good\", \"feedbackImprove\": \"Add more\", " +
            "\"refinedAnswer\": \"Refined\", \"modelAnswer\": \"Model\", " +
            "\"psychologyNote\": \"Note\", \"score\": 6}";

        when(aiProvider.generateJson(any(), any())).thenReturn(responseWithPreamble);

        MentorFeedback feedback = mentorAgent.analyze(SESSION_ID, MESSAGE_ID, "Question", "Answer");

        assertThat(feedback.score()).isEqualTo(6);
        assertThat(feedback.feedbackGood()).isEqualTo("Good");
    }

    @Test
    void analyze_sessionNotFound_throwsSessionException() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mentorAgent.analyze(SESSION_ID, MESSAGE_ID, "Q", "A"))
            .isInstanceOf(com.interviewlab.session.SessionException.class)
            .satisfies(ex -> assertThat(((com.interviewlab.session.SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    void analyze_malformedJsonResponse_throwsParseException() {
        Session session = new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(agentToolChain.execute(any(AgentContext.class))).thenReturn(Map.of());
        when(promptBuilder.buildFeedbackPrompt(any(), any(), any(), any())).thenReturn("Prompt");
        when(aiProvider.generateJson(any(), any())).thenReturn("not valid json at all");

        assertThatThrownBy(() -> mentorAgent.analyze(SESSION_ID, MESSAGE_ID, "Q", "A"))
            .isInstanceOf(com.interviewlab.ai.AIProviderException.class)
            .satisfies(ex -> assertThat(((com.interviewlab.ai.AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.AI_RESPONSE_PARSE_FAILED));
    }
}
