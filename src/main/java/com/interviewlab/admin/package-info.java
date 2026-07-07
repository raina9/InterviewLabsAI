/**
 * Admin-only platform statistics.
 *
 * GET /api/v1/admin/stats — aggregate usage and health numbers (session volume,
 * active users, average answer score, feature usage, AI call budget consumption).
 * All queries are aggregate (COUNT/AVG) — no entity graphs are loaded into memory.
 *
 * Role check follows the same explicit in-controller pattern as
 * com.interviewlab.feedback.SystemFeedbackController — see that class for the
 * @PreAuthorize-vs-explicit-check tradeoff rationale.
 */
package com.interviewlab.admin;
