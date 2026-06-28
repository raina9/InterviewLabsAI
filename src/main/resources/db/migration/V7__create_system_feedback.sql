-- =============================================================================
-- V7 — Create system_feedback table
-- User-submitted feedback about the platform — can be session-scoped or general.
-- applied: no DEFAULT — set from app.feedback.default-applied at service layer.
-- session_id is nullable: feedback may not be tied to a specific session.
-- =============================================================================

CREATE TABLE IF NOT EXISTS system_feedback (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL,
    session_id     UUID,                               -- nullable: general feedback allowed
    feedback_text  TEXT         NOT NULL,
    applied        BOOLEAN      NOT NULL,              -- no DEFAULT: app.feedback.default-applied injected at creation
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_system_feedback         PRIMARY KEY (id),
    CONSTRAINT fk_system_feedback_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_system_feedback_session FOREIGN KEY (session_id) REFERENCES sessions (id)
);

-- Supports findByUserId — all feedback submitted by a user
CREATE INDEX IF NOT EXISTS idx_system_feedback_user_id ON system_feedback (user_id);
