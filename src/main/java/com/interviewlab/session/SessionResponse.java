package com.interviewlab.session;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID          id,
    UUID          userId,
    InterviewType interviewType,
    String        targetCompany,
    String        targetRole,
    String        jdText,
    String        topicFocus,
    String        difficulty,
    SessionStatus status,
    Instant       createdAt,
    Instant       completedAt
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
            session.getId(),
            session.getUserId(),
            session.getInterviewType(),
            session.getTargetCompany(),
            session.getTargetRole(),
            session.getJdText(),
            session.getTopicFocus(),
            session.getDifficulty(),
            session.getStatus(),
            session.getCreatedAt(),
            session.getCompletedAt()
        );
    }
}
