package com.interviewlab.interview;

import com.interviewlab.agent.AgentProperties;
import com.interviewlab.agent.InterviewAgent;
import com.interviewlab.agent.InterviewTurnResult;
import com.interviewlab.agent.MentorAgent;
import com.interviewlab.agent.MentorFeedback;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.event.EventPublisher;
import com.interviewlab.feedback.AnswerFeedback;
import com.interviewlab.feedback.AnswerFeedbackRepository;
import com.interviewlab.session.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock SessionRepository        sessionRepository;
    @Mock SessionService           sessionService;
    @Mock MessageService           messageService;
    @Mock AnswerFeedbackRepository answerFeedbackRepository;
    @Mock InterviewAgent           interviewAgent;
    @Mock MentorAgent              mentorAgent;
    @Mock EventPublisher           eventPublisher;
    @Mock AgentProperties          agentProperties;

    @InjectMocks InterviewService interviewService;

    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID SESSION_ID  = UUID.randomUUID();
    private static final UUID MESSAGE_ID  = UUID.randomUUID();

    private Session activeSession() {
        return new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
    }

    private MentorFeedback sampleFeedback() {
        return new MentorFeedback("Good structure", "Add examples", "Refined: ...", "Model: ...", "Shows clarity", 7);
    }

    // -------------------------------------------------------------------------
    // startInterview
    // -------------------------------------------------------------------------

    @Test
    void startInterview_validSession_returnsFirstQuestion() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(List.of());
        when(interviewAgent.initSession(USER_ID, SESSION_ID)).thenReturn("Tell me about your Java experience.");
        when(agentProperties.totalQuestions()).thenReturn(10);

        InterviewStartResponse response = interviewService.startInterview(USER_ID, SESSION_ID);

        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.firstQuestion()).isEqualTo("Tell me about your Java experience.");
        assertThat(response.totalQuestions()).isEqualTo(10);
        verify(interviewAgent).initSession(USER_ID, SESSION_ID);
    }

    @Test
    void startInterview_sessionNotFound_throwsSessionNotFound() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.startInterview(USER_ID, SESSION_ID))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    void startInterview_notOwned_throwsSessionAccessDenied() {
        UUID otherId = UUID.randomUUID();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));

        assertThatThrownBy(() -> interviewService.startInterview(otherId, SESSION_ID))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_ACCESS_DENIED));
    }

    @Test
    void startInterview_sessionNotActive_throwsInterviewSessionNotActive() {
        Session completed = new Session(USER_ID, InterviewType.TECHNICAL, "Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        completed.setStatus(SessionStatus.COMPLETED);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> interviewService.startInterview(USER_ID, SESSION_ID))
            .isInstanceOf(InterviewException.class)
            .satisfies(ex -> assertThat(((InterviewException) ex).errorCode())
                .isEqualTo(ErrorCode.INTERVIEW_SESSION_NOT_ACTIVE));
    }

    @Test
    void startInterview_alreadyStarted_throwsInterviewAlreadyStarted() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));
        Message existingMsg = new Message(SESSION_ID, MessageRole.INTERVIEWER, "First question", 1, false);
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(List.of(existingMsg));

        assertThatThrownBy(() -> interviewService.startInterview(USER_ID, SESSION_ID))
            .isInstanceOf(InterviewException.class)
            .satisfies(ex -> assertThat(((InterviewException) ex).errorCode())
                .isEqualTo(ErrorCode.INTERVIEW_ALREADY_STARTED));

        verify(interviewAgent, never()).initSession(any(), any());
    }

    // -------------------------------------------------------------------------
    // respond
    // -------------------------------------------------------------------------

    @Test
    void respond_validTurn_persistsFeedbackAndPublishesEvent() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));

        Message lastQuestion = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Explain GC", 1, false);
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(List.of(lastQuestion));

        InterviewTurnResult turnResult = new InterviewTurnResult("Next question?", false, 1, MESSAGE_ID);
        when(interviewAgent.nextTurn(USER_ID, SESSION_ID, "My answer", false)).thenReturn(turnResult);

        MentorFeedback feedback = sampleFeedback();
        when(mentorAgent.analyze(SESSION_ID, MESSAGE_ID, "Explain GC", "My answer")).thenReturn(feedback);

        AnswerFeedback savedFeedback = new AnswerFeedback(
            SESSION_ID, MESSAGE_ID, "Explain GC", "My answer",
            feedback.refinedAnswer(), feedback.modelAnswer(), feedback.score(),
            feedback.feedbackGood(), feedback.feedbackImprove(), feedback.psychologyNote()
        );
        when(answerFeedbackRepository.save(any(AnswerFeedback.class))).thenReturn(savedFeedback);

        InterviewTurnResponse response = interviewService.respond(USER_ID, SESSION_ID, "My answer", false);

        assertThat(response.agentResponse()).isEqualTo("Next question?");
        assertThat(response.sessionComplete()).isFalse();
        assertThat(response.mentorFeedback()).isNotNull();
        assertThat(response.mentorFeedback().score()).isEqualTo(7);
        assertThat(response.mentorFeedback().feedbackGood()).isEqualTo("Good structure");

        verify(answerFeedbackRepository).save(any(AnswerFeedback.class));
        verify(eventPublisher).publishAnswerScored(SESSION_ID, MESSAGE_ID);
        verify(sessionService, never()).completeSession(any(), any());
    }

    @Test
    void respond_finalQuestionOfTotal_autoCompletesSession() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));

        Message lastQuestion = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Explain GC", 1, false);
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(List.of(lastQuestion));

        InterviewTurnResult turnResult = new InterviewTurnResult("Closing remarks.", true, 3, MESSAGE_ID);
        when(interviewAgent.nextTurn(USER_ID, SESSION_ID, "My final answer", false)).thenReturn(turnResult);

        MentorFeedback feedback = sampleFeedback();
        when(mentorAgent.analyze(SESSION_ID, MESSAGE_ID, "Explain GC", "My final answer")).thenReturn(feedback);

        AnswerFeedback savedFeedback = new AnswerFeedback(
            SESSION_ID, MESSAGE_ID, "Explain GC", "My final answer",
            feedback.refinedAnswer(), feedback.modelAnswer(), feedback.score(),
            feedback.feedbackGood(), feedback.feedbackImprove(), feedback.psychologyNote()
        );
        when(answerFeedbackRepository.save(any(AnswerFeedback.class))).thenReturn(savedFeedback);

        InterviewTurnResponse response = interviewService.respond(USER_ID, SESSION_ID, "My final answer", false);

        assertThat(response.sessionComplete()).isTrue();
        verify(sessionService).completeSession(SESSION_ID, USER_ID);
    }

    @Test
    void respond_sessionNotActive_throwsInterviewSessionNotActive() {
        Session abandoned = new Session(USER_ID, InterviewType.TECHNICAL, "Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        abandoned.setStatus(SessionStatus.ABANDONED);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(abandoned));

        assertThatThrownBy(() -> interviewService.respond(USER_ID, SESSION_ID, "answer", false))
            .isInstanceOf(InterviewException.class)
            .satisfies(ex -> assertThat(((InterviewException) ex).errorCode())
                .isEqualTo(ErrorCode.INTERVIEW_SESSION_NOT_ACTIVE));

        verify(interviewAgent, never()).nextTurn(any(), any(), any(), anyBoolean());
    }

    @Test
    void respond_notOwned_throwsSessionAccessDenied() {
        UUID otherId = UUID.randomUUID();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));

        assertThatThrownBy(() -> interviewService.respond(otherId, SESSION_ID, "answer", false))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_ACCESS_DENIED));
    }

    // -------------------------------------------------------------------------
    // getFeedback
    // -------------------------------------------------------------------------

    @Test
    void getFeedback_ownedSession_returnsAllFeedback() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));
        AnswerFeedback f1 = new AnswerFeedback(SESSION_ID, MESSAGE_ID, "Q1", "A1", "R1", "M1", 8, "Good", "Improve", "Note");
        AnswerFeedback f2 = new AnswerFeedback(SESSION_ID, UUID.randomUUID(), "Q2", "A2", "R2", "M2", 6, "Ok", "More detail", "Note2");
        when(answerFeedbackRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(f1, f2));

        List<MentorFeedbackResponse> result = interviewService.getFeedback(USER_ID, SESSION_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).score()).isEqualTo(8);
        assertThat(result.get(1).score()).isEqualTo(6);
    }

    @Test
    void getFeedback_notOwned_throwsSessionAccessDenied() {
        UUID otherId = UUID.randomUUID();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(activeSession()));

        assertThatThrownBy(() -> interviewService.getFeedback(otherId, SESSION_ID))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_ACCESS_DENIED));
    }

    @Test
    void getFeedback_sessionNotFound_throwsSessionNotFound() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getFeedback(USER_ID, SESSION_ID))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }
}
