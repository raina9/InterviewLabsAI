/**
 * Application event system — Observer pattern with V1/V2 evolution path.
 *
 * V1 (active): Synchronous in-process Spring ApplicationEvents.
 *   EventPublisher wraps ApplicationEventPublisher.
 *   Events fire synchronously on the same thread — simple, zero-dependency.
 *   Listeners annotated with @EventListener in their respective packages.
 *
 * V2 (parked — Kafka): KafkaEventPublisher replaces EventPublisher.
 *   Unpark trigger: multi-instance Railway deploy OR async scoring demand.
 *   Topic naming: interview-lab.answer.scored | interview-lab.session.completed
 *   AWS equivalent: SQSMessagePublisher → aws-sdk/sqs (plug-in ready when trigger fires)
 *
 * Events:
 *   AnswerScoredEvent   — fired after MentorAgent persists feedback to answer_feedback
 *   SessionCompletedEvent — fired when session transitions to COMPLETED state
 *
 * The evolution from V1 to V2 requires only swapping the EventPublisher bean —
 * all listener code remains unchanged (decoupled from transport mechanism).
 */
package com.interviewlab.event;
