-- =============================================================================
-- V9 — Add role to users
-- G2 fix: admin endpoint to toggle system_feedback.applied needs an ADMIN/CANDIDATE
-- distinction to enforce. DB-level DEFAULT 'CANDIDATE' (unlike most columns in this
-- schema — see V4/V7 comments) is intentional here: this backfills every pre-existing
-- row in one ALTER TABLE without a separate UPDATE statement. The entity also carries
-- a Java-side default (User.role = Role.CANDIDATE) so Hibernate always sends an explicit
-- value on INSERT — the DB default is a backfill/safety-net, not the primary mechanism.
-- =============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE';

-- Seed dev user (see V8__seed_dev_user.sql) as ADMIN so local dev has an admin
-- account with zero extra setup.
UPDATE users SET role = 'ADMIN' WHERE id = '00000000-0000-0000-0000-000000000001';
