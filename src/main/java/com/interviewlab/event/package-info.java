/**
 * Application event system — Swappable Backend Pattern (Observer pattern).
 *
 * Active (default): SyncEventPublisher
 *   Condition: @ConditionalOnProperty(name="app.messaging.mode", havingValue="sync", matchIfMissing=true)
 *   Config:    MESSAGING_MODE=sync (or unset)
 *   Behaviour: Events fire synchronously on the calling thread — zero infra dependency.
 *   Cost:      zero
 *
 * Future (Kafka): KafkaEventPublisher (not yet built — seam is ready)
 *   Condition: @ConditionalOnProperty(name="app.messaging.mode", havingValue="kafka")
 *   Config:    MESSAGING_MODE=kafka + KAFKA_BOOTSTRAP_SERVERS pointing to a real broker
 *   Unpark trigger: multi-instance Railway deploy OR async scoring demand
 *   Topic naming: interview-lab.answer.scored | interview-lab.session.completed
 *   AWS equivalent: SQSMessagePublisher → aws-sdk/sqs (plug-in ready when trigger fires)
 *   Activation steps:
 *     1. Create KafkaEventPublisher implementing EventPublisher
 *     2. Annotate with @Component @ConditionalOnProperty(name="app.messaging.mode", havingValue="kafka")
 *     3. Set MESSAGING_MODE=kafka and KAFKA_BOOTSTRAP_SERVERS in environment
 *
 * Events:
 *   AnswerScoredEvent     — fired after MentorAgent persists feedback to answer_feedback
 *   SessionCompletedEvent — fired when session transitions to COMPLETED state
 *
 * Bean swap is transparent to all callers — all services inject EventPublisher, not the concrete class.
 */
package com.interviewlab.event;
