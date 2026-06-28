/**
 * Answer feedback persistence and retrieval.
 *
 * Stores the complete output of MentorAgent per candidate answer:
 *   - feedback         — what was missing or weak in the answer
 *   - refined_answer   — the candidate's answer improved in structure and content
 *   - model_answer     — ideal self-contained answer written for interviewer psychology
 *   - psychology_note  — why interviewers react positively or negatively to this answer style
 *
 * Also handles system feedback (user ratings and free-text per session):
 *   - session_id, user_rating (1–5), free_text, submitted_at
 *
 * Tables: answer_feedback, system_feedback
 *
 * MentorAgent writes to this package post-evaluation.
 * EventPublisher fires answer.scored after persistence.
 */
package com.interviewlab.feedback;
