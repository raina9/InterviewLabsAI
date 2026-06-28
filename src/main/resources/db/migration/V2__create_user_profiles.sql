-- =============================================================================
-- V2 — Create user_profiles table
-- 1:1 with users. user_id is both PK and FK — shared primary key pattern.
-- preferred_ai_provider: no DEFAULT — set from app.ai.default-provider at service layer.
-- tech_stack: PostgreSQL native text array for multi-value storage without join table.
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_profiles (
    user_id                UUID         NOT NULL,
    experience_years       INT,
    current_position       TEXT,
    tech_stack             TEXT[],
    resume_text            TEXT,
    custom_prompt          TEXT,
    preferred_ai_provider  TEXT,                       -- no DEFAULT: app.ai.default-provider injected at creation
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_profiles      PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Explicit index on PK column — supports JPA findByUserId() and profile lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles (user_id);
