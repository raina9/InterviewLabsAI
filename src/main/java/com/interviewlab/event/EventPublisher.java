package com.interviewlab.event;

import java.util.UUID;

/**
 * Observer interface for domain events (Observer pattern).
 * V1: SyncEventPublisher — logs events in-process.
 * V2: KafkaEventPublisher — publishes to Kafka topics (unpark trigger: multi-instance or async scoring).
 */
public interface EventPublisher {

    void publishSessionCompleted(UUID sessionId);

    void publishAnswerScored(UUID sessionId, UUID messageId);
}
