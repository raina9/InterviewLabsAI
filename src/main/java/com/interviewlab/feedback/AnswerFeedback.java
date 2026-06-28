package com.interviewlab.feedback;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * AnswerFeedback entity — complete MentorAgent output per candidate answer.
 * score must be 0–10; enforced by DB CHECK constraint and service validation.
 * Nullable feedback fields allow partial AI responses without breaking persistence.
 * No Lombok: JPA entities benefit from explicit constructor visibility control.
 */
@Entity
@Table(
    name = "answer_feedback",
    indexes = @Index(name = "idx_answer_feedback_session_id", columnList = "session_id")
)
public class AnswerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "candidate_answer", nullable = false, columnDefinition = "TEXT")
    private String candidateAnswer;

    @Column(name = "refined_answer", columnDefinition = "TEXT")
    private String refinedAnswer;

    @Column(name = "model_answer", columnDefinition = "TEXT")
    private String modelAnswer;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "feedback_good", columnDefinition = "TEXT")
    private String feedbackGood;

    @Column(name = "feedback_improve", columnDefinition = "TEXT")
    private String feedbackImprove;

    @Column(name = "psychology_note", columnDefinition = "TEXT")
    private String psychologyNote;

    @Column(name = "scored_at", nullable = false, updatable = false)
    private Instant scoredAt;

    protected AnswerFeedback() {}

    public AnswerFeedback(UUID sessionId, UUID messageId, String question, String candidateAnswer,
                          String refinedAnswer, String modelAnswer, int score,
                          String feedbackGood, String feedbackImprove, String psychologyNote) {
        this.sessionId       = sessionId;
        this.messageId       = messageId;
        this.question        = question;
        this.candidateAnswer = candidateAnswer;
        this.refinedAnswer   = refinedAnswer;
        this.modelAnswer     = modelAnswer;
        this.score           = score;
        this.feedbackGood    = feedbackGood;
        this.feedbackImprove = feedbackImprove;
        this.psychologyNote  = psychologyNote;
        this.scoredAt        = Instant.now();
    }

    public UUID getId()                { return id; }
    public UUID getSessionId()         { return sessionId; }
    public UUID getMessageId()         { return messageId; }
    public String getQuestion()        { return question; }
    public String getCandidateAnswer() { return candidateAnswer; }
    public String getRefinedAnswer()   { return refinedAnswer; }
    public String getModelAnswer()     { return modelAnswer; }
    public int getScore()              { return score; }
    public String getFeedbackGood()    { return feedbackGood; }
    public String getFeedbackImprove() { return feedbackImprove; }
    public String getPsychologyNote()  { return psychologyNote; }
    public Instant getScoredAt()       { return scoredAt; }
}
