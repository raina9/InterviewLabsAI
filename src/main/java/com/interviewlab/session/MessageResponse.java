package com.interviewlab.session;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
    UUID        id,
    UUID        sessionId,
    MessageRole role,
    String      content,
    int         sequence,
    boolean     voiceUsed,
    Instant     createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getSessionId(),
            message.getRole(),
            message.getContent(),
            message.getSequence(),
            message.isVoiceUsed(),
            message.getCreatedAt()
        );
    }
}
