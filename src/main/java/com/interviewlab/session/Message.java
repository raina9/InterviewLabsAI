package com.interviewlab.session;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Message entity — one transcript entry within a session.
 * voice_used: no hardcoded default — injected from MessageProperties at creation.
 * sequence is 1-based and determines retrieval order via findBySessionIdOrderBySequence.
 * No Lombok: JPA entities benefit from explicit constructor visibility control.
 */
@Entity
@Table(
    name = "messages",
    indexes = @Index(name = "idx_messages_session_id_sequence", columnList = "session_id, sequence")
)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "voice_used", nullable = false)
    private boolean voiceUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {}

    public Message(UUID sessionId, MessageRole role, String content, int sequence, boolean voiceUsed) {
        this.sessionId = sessionId;
        this.role      = role;
        this.content   = content;
        this.sequence  = sequence;
        this.voiceUsed = voiceUsed;
        this.createdAt = Instant.now();
    }

    public UUID getId()          { return id; }
    public UUID getSessionId()   { return sessionId; }
    public MessageRole getRole() { return role; }
    public String getContent()   { return content; }
    public int getSequence()     { return sequence; }
    public boolean isVoiceUsed() { return voiceUsed; }
    public Instant getCreatedAt() { return createdAt; }
}
