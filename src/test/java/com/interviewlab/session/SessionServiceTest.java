package com.interviewlab.session;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.event.EventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock SessionRepository  sessionRepository;
    @Mock SessionProperties  sessionProperties;
    @Mock EventPublisher     eventPublisher;

    // Real registry, not a mock — constructed manually below rather than via @InjectMocks
    // (same reasoning as the @ConfigurationProperties-record cases in CLAUDE.md: a mocked
    // MeterRegistry.counter(...) would return null and NPE on .increment()).
    SimpleMeterRegistry meterRegistry;
    SessionService       sessionService;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        sessionService = new SessionService(sessionRepository, sessionProperties, eventPublisher, meterRegistry);
    }

    private Session activeSession() {
        return new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD text", "MEDIUM", SessionStatus.ACTIVE);
    }

    @Test
    void createSession_savesAndReturnsSession() {
        when(sessionProperties.defaultStatus()).thenReturn(SessionStatus.ACTIVE);
        Session saved = activeSession();
        when(sessionRepository.save(any(Session.class))).thenReturn(saved);

        CreateSessionRequest request = new CreateSessionRequest(
            InterviewType.TECHNICAL, "Senior Engineer", "JD text", "MEDIUM", null, null
        );
        Session result = sessionService.createSession(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void createSession_incrementsInterviewSessionsCreatedCounter() {
        when(sessionProperties.defaultStatus()).thenReturn(SessionStatus.ACTIVE);
        when(sessionRepository.save(any(Session.class))).thenReturn(activeSession());

        CreateSessionRequest request = new CreateSessionRequest(
            InterviewType.TECHNICAL, "Senior Engineer", "JD text", "MEDIUM", null, null
        );
        sessionService.createSession(USER_ID, request);

        assertThat(meterRegistry.counter("interview.sessions.created").count()).isEqualTo(1.0);
    }

    @Test
    void getSession_ownedByUser_returnsSession() {
        Session session = activeSession();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        Session result = sessionService.getSession(SESSION_ID, USER_ID);

        assertThat(result).isSameAs(session);
    }

    @Test
    void getSession_notFound_throwsSessionNotFound() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSession(SESSION_ID, USER_ID))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    void getSession_notOwned_throwsSessionAccessDenied() {
        UUID otherUser = UUID.randomUUID();
        Session session = activeSession(); // owned by USER_ID
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.getSession(SESSION_ID, otherUser))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_ACCESS_DENIED));
    }

    @Test
    void completeSession_activeSession_completesAndPublishesEvent() {
        Session session = activeSession();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        Session result = sessionService.completeSession(SESSION_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(eventPublisher).publishSessionCompleted(SESSION_ID);
    }

    @Test
    void completeSession_nonActiveSession_throwsSessionNotActive() {
        Session completed = new Session(USER_ID, InterviewType.HR, "PM", "JD", "EASY", SessionStatus.ACTIVE);
        completed.setStatus(SessionStatus.COMPLETED);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> sessionService.completeSession(SESSION_ID, USER_ID))
            .isInstanceOf(SessionException.class)
            .satisfies(ex -> assertThat(((SessionException) ex).errorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_ACTIVE));

        verify(eventPublisher, never()).publishSessionCompleted(any());
    }

    @Test
    void abandonSession_activeSession_transitionsToAbandoned() {
        Session session = activeSession();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        Session result = sessionService.abandonSession(SESSION_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.ABANDONED);
    }

    @Test
    void getUserSessions_returnsAllSessionsForUser() {
        List<Session> sessions = List.of(activeSession(), activeSession());
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(sessions);

        List<Session> result = sessionService.getUserSessions(USER_ID);

        assertThat(result).hasSize(2);
    }
}
