-- =============================================================================
-- V10 — Add CHECK constraints for data integrity (G7 fix)
-- Clean-then-constrain pattern: dirty existing rows are normalised BEFORE the
-- constraint is added, so this migration succeeds on any existing local/prod DB
-- state instead of failing with a constraint-violation error on old data.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- sessions.status — must be one of the SessionStatus enum values
-- -----------------------------------------------------------------------------

-- Step 1: clean — anything outside the valid set is treated as abandoned
UPDATE sessions SET status = 'ABANDONED' WHERE status NOT IN ('ACTIVE', 'COMPLETED', 'ABANDONED');

-- Step 2: constrain
ALTER TABLE sessions ADD CONSTRAINT chk_session_status
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABANDONED'));

-- -----------------------------------------------------------------------------
-- answer_feedback.score — already has an inline, unnamed CHECK from V5 (this
-- table cannot actually contain dirty data), but the clamp is included for
-- consistency with the pattern applied everywhere else in this migration, and
-- the named constraint gives it a predictable name matching chk_session_status.
-- -----------------------------------------------------------------------------

-- Step 3: clamp out-of-range values
UPDATE answer_feedback SET score = 0  WHERE score < 0;
UPDATE answer_feedback SET score = 10 WHERE score > 10;

-- Step 4: constrain (named — supersedes the anonymous inline CHECK from V5;
-- Postgres allows both to coexist harmlessly)
ALTER TABLE answer_feedback ADD CONSTRAINT chk_feedback_score
    CHECK (score >= 0 AND score <= 10);

-- -----------------------------------------------------------------------------
-- proficiency.score / sessions_count — score shares the 0-10 scale used
-- throughout the mentor scoring pipeline (see AssessmentService.levelFor);
-- sessions_count is a non-negative running count with no upper bound.
-- -----------------------------------------------------------------------------

-- Step 5: clean + constrain proficiency columns
UPDATE proficiency SET score = 0  WHERE score < 0;
UPDATE proficiency SET score = 10 WHERE score > 10;
UPDATE proficiency SET sessions_count = 0 WHERE sessions_count < 0;

ALTER TABLE proficiency ADD CONSTRAINT chk_proficiency_score
    CHECK (score >= 0 AND score <= 10);
ALTER TABLE proficiency ADD CONSTRAINT chk_proficiency_sessions_count
    CHECK (sessions_count >= 0);
