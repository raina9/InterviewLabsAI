package com.interviewlab.proficiency;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Proficiency entity — per-user, per-topic competency score.
 * score and sessionsCount: no hardcoded defaults — injected from ProficiencyProperties at creation.
 * UNIQUE (user_id, topic): one row per topic per user — upsert on each mentor loop cycle.
 * No Lombok: JPA entities benefit from explicit constructor visibility control.
 */
@Entity
@Table(
    name = "proficiency",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_proficiency_user_topic",
        columnNames = {"user_id", "topic"}
    ),
    indexes = @Index(name = "idx_proficiency_user_id", columnList = "user_id")
)
public class Proficiency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "sessions_count", nullable = false)
    private int sessionsCount;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected Proficiency() {}

    public Proficiency(UUID userId, String topic, double score, int sessionsCount) {
        this.userId        = userId;
        this.topic         = topic;
        this.score         = score;
        this.sessionsCount = sessionsCount;
        this.lastUpdated   = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.lastUpdated = Instant.now();
    }

    public UUID getId()          { return id; }
    public UUID getUserId()      { return userId; }
    public String getTopic()     { return topic; }
    public double getScore()     { return score; }
    public int getSessionsCount() { return sessionsCount; }
    public Instant getLastUpdated() { return lastUpdated; }

    public void setScore(double score)             { this.score = score; }
    public void setSessionsCount(int sessionsCount) { this.sessionsCount = sessionsCount; }
}
