/**
 * Interview flow — end-to-end interview session lifecycle.
 *
 * Responsibilities:
 *   InterviewController  — REST surface: start, respond, feedback endpoints
 *   InterviewService     — orchestrates agents + persistence for each turn
 *   InterviewException   — interview-flow-specific exception (INTERVIEW_SESSION_NOT_ACTIVE,
 *                          INTERVIEW_ALREADY_STARTED)
 *
 * Per-turn flow (respond):
 *   1. Ownership + ACTIVE guard
 *   2. InterviewAgent.nextTurn() — generates follow-up question, persists CANDIDATE + INTERVIEWER messages
 *   3. MentorAgent.analyze()     — evaluates candidate answer (sync V1)
 *   4. Persist AnswerFeedback
 *   5. Publish answer.scored event
 *
 * V2 note: MentorAgent.analyze() moves to async (Kafka) when multi-instance scoring is needed.
 */
package com.interviewlab.interview;
