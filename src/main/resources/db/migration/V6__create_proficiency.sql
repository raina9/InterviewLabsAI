-- =============================================================================
-- V6 — Create proficiency table
-- Tracks per-user competency per topic across sessions.
-- score and sessions_count: no DEFAULT — set from app.proficiency.* at service layer.
-- UNIQUE (user_id, topic): one row per topic per user — upsert on scoring cycle.
-- =============================================================================

CREATE TABLE IF NOT EXISTS proficiency (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    topic           TEXT         NOT NULL,
    score           FLOAT        NOT NULL,             -- no DEFAULT: app.proficiency.default-score injected at creation
    sessions_count  INT          NOT NULL,             -- no DEFAULT: app.proficiency.default-sessions-count injected at creation
    last_updated    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_proficiency             PRIMARY KEY (id),
    CONSTRAINT fk_proficiency_user        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_proficiency_user_topic  UNIQUE (user_id, topic)
);

-- Supports findByUserId — full proficiency profile for a candidate
CREATE INDEX IF NOT EXISTS idx_proficiency_user_id ON proficiency (user_id);
