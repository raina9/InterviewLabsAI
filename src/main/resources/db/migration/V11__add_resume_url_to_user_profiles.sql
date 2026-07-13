-- =============================================================================
-- V11 — Add resume_url column to user_profiles (G6: resume upload endpoint)
-- Stores the URL returned by StorageService after a PDF resume upload — distinct
-- from resume_text (existing column: plain-text ATS paste used as AI context).
-- =============================================================================

ALTER TABLE user_profiles ADD COLUMN resume_url TEXT;
