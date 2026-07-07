package com.interviewlab.session;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.event.EventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SessionService {

    private final SessionRepository  sessionRepository;
    private final SessionProperties  sessionProperties;
    private final EventPublisher     eventPublisher;
    private final MeterRegistry      meterRegistry;

    @Transactional
    public Session createSession(UUID userId, CreateSessionRequest request) {
        Session session = new Session(
            userId,
            request.interviewType(),
            request.targetRole(),
            request.jdText(),
            request.difficulty(),
            sessionProperties.defaultStatus()
        );
        session.setTargetCompany(request.targetCompany());
        session.setTopicFocus(request.topicFocus());
        Session saved = sessionRepository.save(session);
        meterRegistry.counter("interview.sessions.created").increment();
        log.info("Session created: id={} userId={} type={}", saved.getId(), userId, request.interviewType());
        return saved;
    }

    @Transactional(readOnly = true)
    public Session getSession(UUID sessionId, UUID userId) {
        Session session = findSessionOrThrow(sessionId);
        assertOwnership(session, userId);
        return session;
    }

    @Transactional(readOnly = true)
    public List<Session> getUserSessions(UUID userId) {
        return sessionRepository.findByUserId(userId);
    }

    @Transactional
    public Session completeSession(UUID sessionId, UUID userId) {
        Session session = findSessionOrThrow(sessionId);
        assertOwnership(session, userId);
        assertActive(session);
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        Session saved = sessionRepository.save(session);
        log.info("Session completed: id={} userId={}", sessionId, userId);
        eventPublisher.publishSessionCompleted(sessionId);
        return saved;
    }

    @Transactional
    public Session abandonSession(UUID sessionId, UUID userId) {
        Session session = findSessionOrThrow(sessionId);
        assertOwnership(session, userId);
        assertActive(session);
        session.setStatus(SessionStatus.ABANDONED);
        Session saved = sessionRepository.save(session);
        log.info("Session abandoned: id={} userId={}", sessionId, userId);
        return saved;
    }

    private Session findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionException(
                ErrorCode.SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Session " + sessionId + " not found"
            ));
    }

    private void assertOwnership(Session session, UUID userId) {
        if (!session.getUserId().equals(userId)) {
            throw new SessionException(
                ErrorCode.SESSION_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                "Session " + session.getId() + " does not belong to the requesting user"
            );
        }
    }

    private void assertActive(Session session) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionException(
                ErrorCode.SESSION_NOT_ACTIVE,
                HttpStatus.CONFLICT,
                "Session " + session.getId() + " is in status " + session.getStatus() + " and cannot be modified"
            );
        }
    }
}
