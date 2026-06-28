/**
 * Candidate proficiency tracking — evolves after every mentor loop cycle.
 *
 * Tracks per-user competency over time:
 *   - english_score    — 0.0–1.0, based on clarity, grammar, and communication quality
 *   - domain_scores    — JSONB map: { "java": 0.72, "system-design": 0.45, ... }
 *   - last_updated     — timestamp of last MentorAgent scoring cycle
 *
 * The proficiency profile is read by ProficiencyTool (agent package) to personalise
 * agent context — e.g. the MentorAgent adjusts feedback depth based on domain score.
 *
 * Table: proficiency (1:1 with users)
 *
 * Update flow: MentorAgent scores answer → proficiency service updates scores.
 * Domain scores use JSONB to allow open-ended topic coverage without schema changes.
 */
package com.interviewlab.proficiency;
