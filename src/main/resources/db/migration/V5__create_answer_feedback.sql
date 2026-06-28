-- =============================================================================
-- V5 — Create answer_feedback table
-- Persists the full MentorAgent output per candidate answer.
-- score CHECK (0–10): enforced at DB layer; also validated at service layer.
-- All feedback fields are nullable — partial feedback is valid if scoring succeeds.
-- =============================================================================

CREATE TABLE IF NOT EXISTS answer_feedback (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    session_id       UUID         NOT NULL,
    message_id       UUID         NOT NULL,
    question         TEXT         NOT NULL,
    candidate_answer TEXT         NOT NULL,
    refined_answer   TEXT,
    model_answer     TEXT,
    score            INT          NOT NULL CHECK (score >= 0 AND score <= 10),
    feedback_good    TEXT,
    feedback_improve TEXT,
    psychology_note  TEXT,
    scored_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_answer_feedback         PRIMARY KEY (id),
    CONSTRAINT fk_answer_feedback_session FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_answer_feedback_message FOREIGN KEY (message_id) REFERENCES messages (id)
);

-- Supports findBySessionId — all feedback for a completed session review
CREATE INDEX IF NOT EXISTS idx_answer_feedback_session_id ON answer_feedback (session_id);
