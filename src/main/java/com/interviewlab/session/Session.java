package com.interviewlab.session;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Session entity — one interview run per user.
 * status: no hardcoded default — injected from SessionProperties at creation.
 * target_company and topic_focus are optional inputs, nullable.
 * No Lombok: JPA entities benefit from explicit constructor visibility control.
 */
@Entity
@Table(
    name = "sessions",
    indexes = @Index(name = "idx_sessions_user_id_status", columnList = "user_id, status")
)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false)
    private InterviewType interviewType;

    @Column(name = "target_company")
    private String targetCompany;

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "jd_text", nullable = false, columnDefinition = "TEXT")
    private String jdText;

    @Column(name = "topic_focus")
    private String topicFocus;

    @Column(name = "difficulty", nullable = false)
    private String difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Session() {}

    public Session(UUID userId, InterviewType interviewType, String targetRole,
                   String jdText, String difficulty, SessionStatus status) {
        this.userId        = userId;
        this.interviewType = interviewType;
        this.targetRole    = targetRole;
        this.jdText        = jdText;
        this.difficulty    = difficulty;
        this.status        = status;
        this.createdAt     = Instant.now();
    }

    public UUID getId()                  { return id; }
    public UUID getUserId()              { return userId; }
    public InterviewType getInterviewType() { return interviewType; }
    public String getTargetCompany()     { return targetCompany; }
    public String getTargetRole()        { return targetRole; }
    public String getJdText()            { return jdText; }
    public String getTopicFocus()        { return topicFocus; }
    public String getDifficulty()        { return difficulty; }
    public SessionStatus getStatus()     { return status; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getCompletedAt()      { return completedAt; }

    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }
    public void setTopicFocus(String topicFocus)       { this.topicFocus = topicFocus; }
    public void setStatus(SessionStatus status)        { this.status = status; }
    public void setCompletedAt(Instant completedAt)    { this.completedAt = completedAt; }
}
