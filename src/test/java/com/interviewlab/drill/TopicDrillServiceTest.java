package com.interviewlab.drill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProviderStrategy;
import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicDrillServiceTest {

    @Mock AIProviderFactory  aiProviderFactory;
    @Mock AiProviderStrategy aiProvider;
    @Spy  ObjectMapper       objectMapper = new ObjectMapper();

    @InjectMocks DrillService drillService;

    private static final String RAPID_QUESTIONS_JSON = """
        {
          "questions": [
            "What is the difference between HashMap and Hashtable?",
            "Explain the Java memory model briefly.",
            "What does volatile do in Java?",
            "What is the difference between == and equals()?",
            "What is autoboxing?",
            "Explain method overloading vs overriding.",
            "What is a checked vs unchecked exception?",
            "What is the purpose of the final keyword?",
            "What does synchronized do?",
            "What is a lambda expression?"
          ]
        }
        """;

    private void stubAiProvider() {
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
    }

    // -------------------------------------------------------------------------
    // Scenario 1: RAPID mode — start returns first question from pre-generated list
    // -------------------------------------------------------------------------

    @Test
    void startDrill_rapidMode_returnsFirstQuestion() {
        stubAiProvider();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn(RAPID_QUESTIONS_JSON);

        DrillSession session = drillService.startDrill(new DrillRequest("Java", DrillMode.RAPID));

        assertThat(session.sessionId()).isNotNull();
        assertThat(session.topic()).isEqualTo("Java");
        assertThat(session.mode()).isEqualTo(DrillMode.RAPID);
        assertThat(session.currentQuestion()).isEqualTo("What is the difference between HashMap and Hashtable?");
        assertThat(session.questionsAnswered()).isEqualTo(0);
        assertThat(session.complete()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: DEEP mode — start generates a single socratic question
    // -------------------------------------------------------------------------

    @Test
    void startDrill_deepMode_generatesOpeningQuestion() {
        stubAiProvider();
        when(aiProvider.generate(anyString(), any(AIOptions.class)))
            .thenReturn("Explain the Java memory model and how volatile relates to it.");

        DrillSession session = drillService.startDrill(new DrillRequest("Java", DrillMode.DEEP));

        assertThat(session.mode()).isEqualTo(DrillMode.DEEP);
        assertThat(session.currentQuestion()).isNotBlank();
        verify(aiProvider, never()).generateJson(anyString(), any(AIOptions.class));
    }

    // -------------------------------------------------------------------------
    // Scenario 3: RAPID mode — next evaluates answer and advances to Q2
    // -------------------------------------------------------------------------

    @Test
    void nextQuestion_rapidMode_evaluatesAndAdvances() {
        stubAiProvider();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn(RAPID_QUESTIONS_JSON)    // startDrill: generates questions
            .thenReturn("{\"score\": 7}");        // nextQuestion: evaluate answer
        when(aiProvider.generate(anyString(), any(AIOptions.class)))
            .thenReturn("Good answer — HashMap is not thread-safe while Hashtable is synchronized.");

        DrillSession start = drillService.startDrill(new DrillRequest("Java", DrillMode.RAPID));
        DrillQuestionResponse next = drillService.nextQuestion(
            start.sessionId(), new DrillAnswerRequest("HashMap is not synchronized, Hashtable is.")
        );

        assertThat(next.feedback()).isNotBlank();
        assertThat(next.previousScore()).isEqualTo(7);
        assertThat(next.sessionComplete()).isFalse();
        assertThat(next.question()).isEqualTo("Explain the Java memory model briefly.");
    }

    // -------------------------------------------------------------------------
    // Scenario 4: DEEP mode — follow-up question generated from previous answer
    // -------------------------------------------------------------------------

    @Test
    void nextQuestion_deepMode_generatesFollowUp() {
        stubAiProvider();
        when(aiProvider.generate(anyString(), any(AIOptions.class)))
            .thenReturn("Explain the Java memory model and how volatile relates to it.")  // opening
            .thenReturn("Good but shallow — what specific guarantee does volatile provide?")  // feedback
            .thenReturn("Can you describe a scenario where volatile alone is insufficient?"); // follow-up
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn("{\"score\": 5}");

        DrillSession start = drillService.startDrill(new DrillRequest("Concurrency", DrillMode.DEEP));
        DrillQuestionResponse next = drillService.nextQuestion(
            start.sessionId(), new DrillAnswerRequest("volatile ensures visibility across threads.")
        );

        assertThat(next.sessionComplete()).isFalse();
        assertThat(next.question()).isNotBlank();
        assertThat(next.previousScore()).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // Scenario 5: getSummary — calculates weak spots and strong points correctly
    // -------------------------------------------------------------------------

    @Test
    void getSummary_afterAnswers_returnsWeakAndStrongSpots() {
        stubAiProvider();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn(RAPID_QUESTIONS_JSON)
            .thenReturn("{\"score\": 3}")   // Q1: weak
            .thenReturn("{\"score\": 8}");  // Q2: strong
        when(aiProvider.generate(anyString(), any(AIOptions.class)))
            .thenReturn("feedback 1")
            .thenReturn("follow-up Q3")
            .thenReturn("feedback 2");

        DrillSession start = drillService.startDrill(new DrillRequest("Java", DrillMode.RAPID));
        UUID sid = start.sessionId();

        drillService.nextQuestion(sid, new DrillAnswerRequest("weak answer"));
        drillService.nextQuestion(sid, new DrillAnswerRequest("strong answer"));

        DrillSummary summary = drillService.getSummary(sid);

        assertThat(summary.topic()).isEqualTo("Java");
        assertThat(summary.questionsAnswered()).isEqualTo(2);
        assertThat(summary.weakSpots()).hasSize(1);
        assertThat(summary.strongPoints()).hasSize(1);
        assertThat(summary.avgScore()).isBetween(5.0, 6.0); // (3+8)/2 = 5.5
    }

    // -------------------------------------------------------------------------
    // Scenario 6: nextQuestion on unknown session → DRILL_SESSION_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void nextQuestion_unknownSession_throwsNotFound() {
        assertThatThrownBy(() -> drillService.nextQuestion(
            UUID.randomUUID(), new DrillAnswerRequest("any")
        ))
            .isInstanceOf(DrillException.class)
            .satisfies(ex -> assertThat(((DrillException) ex).errorCode())
                .isEqualTo(ErrorCode.DRILL_SESSION_NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // Scenario 7: nextQuestion on completed session → DRILL_ALREADY_COMPLETED
    // -------------------------------------------------------------------------

    @Test
    void nextQuestion_completedSession_throwsAlreadyCompleted() {
        stubAiProvider();
        // Generate 1-question session to complete it immediately
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn("{\"questions\": [\"Only one question?\"]}")
            .thenReturn("{\"score\": 5}");
        when(aiProvider.generate(anyString(), any(AIOptions.class)))
            .thenReturn("feedback");

        DrillSession start = drillService.startDrill(new DrillRequest("Java", DrillMode.RAPID));
        UUID sid = start.sessionId();

        // Answer all questions — RAPID maxes at 10 but with 1 question the session hits null→complete
        // We need to exhaust the list to trigger completion
        // Rapid questions list has 1 item; nextRapidQuestion() returns null → complete
        DrillQuestionResponse r1 = drillService.nextQuestion(sid, new DrillAnswerRequest("answer"));
        assertThat(r1.sessionComplete()).isTrue();

        assertThatThrownBy(() -> drillService.nextQuestion(sid, new DrillAnswerRequest("too late")))
            .isInstanceOf(DrillException.class)
            .satisfies(ex -> assertThat(((DrillException) ex).errorCode())
                .isEqualTo(ErrorCode.DRILL_ALREADY_COMPLETED));
    }

    // -------------------------------------------------------------------------
    // Scenario 8: RAPID start — AI returns malformed JSON → DRILL_GENERATION_FAILED
    // -------------------------------------------------------------------------

    @Test
    void startDrill_rapidMalformedJson_throwsGenerationFailed() {
        stubAiProvider();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class))).thenReturn("not json at all");

        assertThatThrownBy(() -> drillService.startDrill(new DrillRequest("Java", DrillMode.RAPID)))
            .isInstanceOf(DrillException.class)
            .satisfies(ex -> assertThat(((DrillException) ex).errorCode())
                .isEqualTo(ErrorCode.DRILL_GENERATION_FAILED));
    }

    // -------------------------------------------------------------------------
    // Scenario 9: evaluateAnswer — AI returns malformed score → DRILL_GENERATION_FAILED
    // -------------------------------------------------------------------------

    @Test
    void nextQuestion_malformedScoreJson_throwsGenerationFailed() {
        stubAiProvider();
        when(aiProvider.generateJson(anyString(), any(AIOptions.class)))
            .thenReturn(RAPID_QUESTIONS_JSON)      // startDrill: generates questions
            .thenReturn("not valid json score");   // nextQuestion: evaluate answer — malformed
        when(aiProvider.generate(anyString(), any(AIOptions.class)))
            .thenReturn("feedback");

        DrillSession start = drillService.startDrill(new DrillRequest("Java", DrillMode.RAPID));

        assertThatThrownBy(() -> drillService.nextQuestion(
            start.sessionId(), new DrillAnswerRequest("some answer")
        ))
            .isInstanceOf(DrillException.class)
            .satisfies(ex -> assertThat(((DrillException) ex).errorCode())
                .isEqualTo(ErrorCode.DRILL_GENERATION_FAILED));
    }
}
