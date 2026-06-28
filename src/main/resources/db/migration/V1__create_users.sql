-- =============================================================================
-- V1 — Create users table
-- Identity: Google OAuth2 users identified by google_sub (OIDC sub claim).
-- google_sub is the stable identifier — survives email changes on Google's side.
-- UUID primary key: generated in PostgreSQL via gen_random_uuid() (pgcrypto built-in).
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    google_sub  VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    picture     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users          PRIMARY KEY (id),
    CONSTRAINT uq_users_google_sub UNIQUE (google_sub),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- Supports OAuth2 login lookup path: findByGoogleSub() on every login
CREATE INDEX IF NOT EXISTS idx_users_google_sub ON users (google_sub);

-- Supports email-based lookups and deduplication checks
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
