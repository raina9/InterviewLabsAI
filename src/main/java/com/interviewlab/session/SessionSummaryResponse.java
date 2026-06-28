package com.interviewlab.session;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight session representation for list endpoints — omits jdText to keep payload small.
 */
public record SessionSummaryResponse(
    UUID          id,
    InterviewType interviewType,
    String        targetCompany,
    String        targetRole,
    String        difficulty,
    SessionStatus status,
    Instant       createdAt,
    Instant       completedAt
) {
    public static SessionSummaryResponse from(Session session) {
        return new SessionSummaryResponse(
            session.getId(),
            session.getInterviewType(),
            session.getTargetCompany(),
            session.getTargetRole(),
            session.getDifficulty(),
            session.getStatus(),
            session.getCreatedAt(),
            session.getCompletedAt()
        );
    }
}
