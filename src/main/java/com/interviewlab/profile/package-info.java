/**
 * User profile management — cross-session learning persistence.
 *
 * Stores per-user state that persists and evolves across interview sessions:
 *   - Resume text (ATS paste format)
 *   - Experience level (JUNIOR / MID / SENIOR / STAFF / PRINCIPAL)
 *   - Preferred interview types (HR, TECHNICAL, SYSTEM_DESIGN, BEHAVIOURAL)
 *   - Custom prompt (saved by user, auto-applied to every subsequent session)
 *
 * The custom prompt is a key differentiator — users can save instructions like
 * "focus on system design for distributed systems" and the agent applies it automatically.
 *
 * Table: user_profiles (1:1 with users)
 *
 * ResumeContextTool and UserPromptTool in the agent package read from this package.
 */
package com.interviewlab.profile;
