-- =============================================================================
-- V12 — Seed dev user profile row (V8 seeded the users row, never the 1:1 profile)
-- Idempotent: dev environment gets a complete users + user_profiles pair.
-- App-layer upsert (UserProfileService.updateResumeUrl -> getOrCreateProfile)
-- now also self-heals this for any user, but the dev seed should be complete
-- out of the box rather than relying on the first resume upload to create it.
-- =============================================================================

INSERT INTO user_profiles (user_id, preferred_ai_provider, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'OLLAMA',
  now()
) ON CONFLICT (user_id) DO NOTHING;
