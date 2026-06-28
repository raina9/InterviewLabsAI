package com.interviewlab.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * V1 in-process event publisher — logs events only.
 * Swap to KafkaEventPublisher in V2 without changing any caller (Strategy via interface).
 */
@Slf4j
@Component
public class SyncEventPublisher implements EventPublisher {

    @Override
    public void publishSessionCompleted(UUID sessionId) {
        log.info("event=session.completed sessionId={}", sessionId);
    }

    @Override
    public void publishAnswerScored(UUID sessionId, UUID messageId) {
        log.info("event=answer.scored sessionId={} messageId={}", sessionId, messageId);
    }
}
