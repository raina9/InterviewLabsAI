-- =============================================================================
-- V3 — Create sessions table
-- Represents one interview session per user run.
-- status: no DEFAULT — set from app.session.default-status at service layer.
-- target_company and topic_focus are optional user inputs.
-- =============================================================================

CREATE TABLE IF NOT EXISTS sessions (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    interview_type  TEXT         NOT NULL,
    target_company  TEXT,
    target_role     TEXT         NOT NULL,
    jd_text         TEXT         NOT NULL,
    topic_focus     TEXT,
    difficulty      TEXT         NOT NULL,
    status          TEXT         NOT NULL,              -- no DEFAULT: app.session.default-status injected at creation
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,

    CONSTRAINT pk_sessions      PRIMARY KEY (id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Composite index covers findByUserIdAndStatus (full match) and findByUserId (leftmost prefix)
CREATE INDEX IF NOT EXISTS idx_sessions_user_id_status ON sessions (user_id, status);
