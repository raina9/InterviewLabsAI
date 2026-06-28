package com.interviewlab.feedback;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SystemFeedback entity — user-submitted platform feedback.
 * applied: no hardcoded default — injected from FeedbackProperties at creation.
 * session_id is nullable: feedback may be general (not tied to a specific session).
 * No Lombok: JPA entities benefit from explicit constructor visibility control.
 */
@Entity
@Table(
    name = "system_feedback",
    indexes = @Index(name = "idx_system_feedback_user_id", columnList = "user_id")
)
public class SystemFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "session_id", updatable = false)
    private UUID sessionId;

    @Column(name = "feedback_text", nullable = false, columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "applied", nullable = false)
    private boolean applied;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SystemFeedback() {}

    public SystemFeedback(UUID userId, UUID sessionId, String feedbackText, boolean applied) {
        this.userId       = userId;
        this.sessionId    = sessionId;
        this.feedbackText = feedbackText;
        this.applied      = applied;
        this.createdAt    = Instant.now();
    }

    public UUID getId()             { return id; }
    public UUID getUserId()         { return userId; }
    public UUID getSessionId()      { return sessionId; }
    public String getFeedbackText() { return feedbackText; }
    public boolean isApplied()      { return applied; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setApplied(boolean applied) { this.applied = applied; }
}
