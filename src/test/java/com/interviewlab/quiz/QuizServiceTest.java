package com.interviewlab.quiz;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock AIProviderFactory  aiProviderFactory;
    @Mock AiProviderStrategy aiProvider;
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

    QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(aiProviderFactory, objectMapper, aiProperties);
    }

    private static final String VALID_QUESTIONS_JSON = """
        {
          "questions": [
            {
              "question": "What is the time complexity of HashMap.get()?",
              "options": ["O(1)", "O(n)", "O(log n)", "O(n log n)"],
              "correctAnswer": "O(1)",
              "explanation": "HashMap uses hashing for amortized O(1) lookup."
            },
            {
              "question": "Which data structure uses LIFO ordering?",
              "options": ["Queue", "Stack", "LinkedList", "Tree"],
              "correctAnswer": "Stack",
              "explanation": "Stack follows Last-In-First-Out (LIFO) ordering."
            }
          ]
        }
        """;

    // -------------------------------------------------------------------------
    // Scenario 1: valid AI response → quiz session started with first question
    // -------------------------------------------------------------------------

    @Test
    void startQuiz_validAiResponse_returnsSessionWithFirstQuestion() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_QUESTIONS_JSON);

        QuizSession session = quizService.startQuiz(new StartQuizRequest("Java", "medium", 2));

        assertThat(session.sessionId()).isNotNull();
        assertThat(session.topic()).isEqualTo("Java");
        assertThat(session.totalQuestions()).isEqualTo(2);
        assertThat(session.currentIndex()).isEqualTo(0);
        assertThat(session.score()).isEqualTo(0);
        assertThat(session.currentQuestion()).isEqualTo("What is the time complexity of HashMap.get()?");
        assertThat(session.currentOptions()).contains("O(1)", "O(n)");
    }

    // -------------------------------------------------------------------------
    // Scenario 2: AI returns malformed JSON → QUIZ_GENERATION_FAILED
    // -------------------------------------------------------------------------

    @Test
    void startQuiz_malformedJson_throwsQuizGenerationFailed() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn("not json");

        assertThatThrownBy(() -> quizService.startQuiz(new StartQuizRequest("Java", "easy", 3)))
            .isInstanceOf(QuizException.class)
            .satisfies(ex -> {
                QuizException qe = (QuizException) ex;
                assertThat(qe.errorCode()).isEqualTo(ErrorCode.QUIZ_GENERATION_FAILED);
                assertThat(qe.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            });
    }

    // -------------------------------------------------------------------------
    // Scenario 3: correct answer → score increments, next question returned
    // -------------------------------------------------------------------------

    @Test
    void submitAnswer_correctAnswer_incrementsScoreAndAdvances() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_QUESTIONS_JSON);
        QuizSession session = quizService.startQuiz(new StartQuizRequest("Java", "medium", 2));

        QuizAnswerResponse response = quizService.submitAnswer(session.sessionId(), "O(1)");

        assertThat(response.correct()).isTrue();
        assertThat(response.score()).isEqualTo(1);
        assertThat(response.totalAnswered()).isEqualTo(1);
        assertThat(response.sessionComplete()).isFalse();
        assertThat(response.nextQuestion()).isEqualTo("Which data structure uses LIFO ordering?");
        assertThat(response.correctAnswer()).isEqualTo("O(1)");
    }

    // -------------------------------------------------------------------------
    // Scenario 4: wrong answer → score unchanged, explanation returned
    // -------------------------------------------------------------------------

    @Test
    void submitAnswer_wrongAnswer_scoreUnchangedExplanationReturned() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_QUESTIONS_JSON);
        QuizSession session = quizService.startQuiz(new StartQuizRequest("Java", "medium", 2));

        QuizAnswerResponse response = quizService.submitAnswer(session.sessionId(), "O(n)");

        assertThat(response.correct()).isFalse();
        assertThat(response.score()).isEqualTo(0);
        assertThat(response.explanation()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: answer last question → sessionComplete=true
    // -------------------------------------------------------------------------

    @Test
    void submitAnswer_lastQuestion_sessionComplete() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_QUESTIONS_JSON);
        QuizSession session = quizService.startQuiz(new StartQuizRequest("Java", "medium", 2));
        UUID sid = session.sessionId();

        quizService.submitAnswer(sid, "O(1)");
        QuizAnswerResponse last = quizService.submitAnswer(sid, "Stack");

        assertThat(last.sessionComplete()).isTrue();
        assertThat(last.nextQuestion()).isNull();
    }

    // -------------------------------------------------------------------------
    // Scenario 6: getResult on complete session → correct QuizResult
    // -------------------------------------------------------------------------

    @Test
    void getResult_completedSession_returnsResult() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_QUESTIONS_JSON);
        QuizSession session = quizService.startQuiz(new StartQuizRequest("Java", "medium", 2));
        UUID sid = session.sessionId();

        quizService.submitAnswer(sid, "O(1)");    // correct
        quizService.submitAnswer(sid, "Queue");   // wrong

        QuizResult result = quizService.getResult(sid);

        assertThat(result.totalQuestions()).isEqualTo(2);
        assertThat(result.correctAnswers()).isEqualTo(1);
        assertThat(result.scorePercent()).isEqualTo(50);
    }

    // -------------------------------------------------------------------------
    // Scenario 7: submitAnswer on unknown session → QUIZ_SESSION_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void submitAnswer_unknownSession_throwsSessionNotFound() {
        assertThatThrownBy(() -> quizService.submitAnswer(UUID.randomUUID(), "any"))
            .isInstanceOf(QuizException.class)
            .satisfies(ex -> assertThat(((QuizException) ex).errorCode())
                .isEqualTo(ErrorCode.QUIZ_SESSION_NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // Scenario 8: submitAnswer on completed session → QUIZ_ALREADY_COMPLETED
    // -------------------------------------------------------------------------

    @Test
    void submitAnswer_completedSession_throwsAlreadyCompleted() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(VALID_QUESTIONS_JSON);
        QuizSession session = quizService.startQuiz(new StartQuizRequest("Java", "easy", 2));
        UUID sid = session.sessionId();

        quizService.submitAnswer(sid, "O(1)");
        quizService.submitAnswer(sid, "Stack");

        assertThatThrownBy(() -> quizService.submitAnswer(sid, "O(1)"))
            .isInstanceOf(QuizException.class)
            .satisfies(ex -> assertThat(((QuizException) ex).errorCode())
                .isEqualTo(ErrorCode.QUIZ_ALREADY_COMPLETED));
    }
}
