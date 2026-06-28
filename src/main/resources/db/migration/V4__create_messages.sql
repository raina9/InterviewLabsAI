-- =============================================================================
-- V4 — Create messages table
-- Ordered transcript of a session: INTERVIEWER questions, CANDIDATE answers, MENTOR feedback.
-- voice_used: no DEFAULT — set from app.message.default-voice-used at service layer.
-- sequence: 1-based order within the session for deterministic retrieval.
-- =============================================================================

CREATE TABLE IF NOT EXISTS messages (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    session_id  UUID         NOT NULL,
    role        TEXT         NOT NULL,
    content     TEXT         NOT NULL,
    sequence    INT          NOT NULL,
    voice_used  BOOLEAN      NOT NULL,                 -- no DEFAULT: app.message.default-voice-used injected at creation
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_messages         PRIMARY KEY (id),
    CONSTRAINT fk_messages_session FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE CASCADE
);

-- Composite index covers findBySessionIdOrderBySequence — both filter and sort use the index
CREATE INDEX IF NOT EXISTS idx_messages_session_id_sequence ON messages (session_id, sequence);
