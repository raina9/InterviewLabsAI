package com.interviewlab.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * V1 in-process event publisher — logs events only. Active by default (MESSAGING_MODE=sync).
 * Swap to KafkaEventPublisher by setting MESSAGING_MODE=kafka — no code change required.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "sync", matchIfMissing = true)
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
