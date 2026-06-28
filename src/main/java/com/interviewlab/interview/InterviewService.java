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
import com.interviewlab.session.Message;
import com.interviewlab.session.MessageRole;
import com.interviewlab.session.MessageService;
import com.interviewlab.session.Session;
import com.interviewlab.session.SessionException;
import com.interviewlab.session.SessionRepository;
import com.interviewlab.session.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class InterviewService {

    private final SessionRepository       sessionRepository;
    private final MessageService          messageService;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final InterviewAgent          interviewAgent;
    private final MentorAgent             mentorAgent;
    private final EventPublisher          eventPublisher;
    private final AgentProperties         agentProperties;

    @Transactional
    public InterviewStartResponse startInterview(UUID userId, UUID sessionId) {
        Session session = findSessionOrThrow(sessionId);
        assertOwnership(session, userId);
        assertActive(session, sessionId);

        List<Message> existing = messageService.getSessionMessages(sessionId);
        if (!existing.isEmpty()) {
            throw new InterviewException(
                ErrorCode.INTERVIEW_ALREADY_STARTED,
                HttpStatus.CONFLICT,
                "Interview for session " + sessionId + " has already been started"
            );
        }

        String firstQuestion = interviewAgent.initSession(userId, sessionId);
        log.info("Interview started: sessionId={} userId={}", sessionId, userId);
        return new InterviewStartResponse(sessionId, firstQuestion, agentProperties.totalQuestions());
    }

    @Transactional
    public InterviewTurnResponse respond(UUID userId, UUID sessionId, String answer, boolean voiceUsed) {
        Session session = findSessionOrThrow(sessionId);
        assertOwnership(session, userId);
        assertActive(session, sessionId);

        String lastQuestion = messageService.getSessionMessages(sessionId).stream()
            .filter(m -> m.getRole() == MessageRole.INTERVIEWER)
            .reduce((a, b) -> b)
            .map(Message::getContent)
            .orElse("");

        InterviewTurnResult turnResult = interviewAgent.nextTurn(userId, sessionId, answer, voiceUsed);

        MentorFeedback mentorFeedback = mentorAgent.analyze(
            sessionId, turnResult.candidateMessageId(), lastQuestion, answer
        );

        answerFeedbackRepository.save(new AnswerFeedback(
            sessionId,
            turnResult.candidateMessageId(),
            lastQuestion,
            answer,
            mentorFeedback.refinedAnswer(),
            mentorFeedback.modelAnswer(),
            mentorFeedback.score(),
            mentorFeedback.feedbackGood(),
            mentorFeedback.feedbackImprove(),
            mentorFeedback.psychologyNote()
        ));

        eventPublisher.publishAnswerScored(sessionId, turnResult.candidateMessageId());
        log.info("Interview turn completed: sessionId={} score={}", sessionId, mentorFeedback.score());

        return new InterviewTurnResponse(
            turnResult.agentResponse(),
            turnResult.shouldMoveToNextQuestion(),
            MentorFeedbackResponse.from(mentorFeedback)
        );
    }

    @Transactional(readOnly = true)
    public List<MentorFeedbackResponse> getFeedback(UUID userId, UUID sessionId) {
        Session session = findSessionOrThrow(sessionId);
        assertOwnership(session, userId);
        return answerFeedbackRepository.findBySessionId(sessionId).stream()
            .map(MentorFeedbackResponse::from)
            .toList();
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

    private void assertActive(Session session, UUID sessionId) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InterviewException(
                ErrorCode.INTERVIEW_SESSION_NOT_ACTIVE,
                HttpStatus.CONFLICT,
                "Session " + sessionId + " is not ACTIVE and cannot accept interview turns"
            );
        }
    }
}
