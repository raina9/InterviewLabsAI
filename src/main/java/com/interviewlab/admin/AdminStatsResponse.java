package com.interviewlab.admin;

import java.util.Map;

/**
 * featureUsage counts are lifetime totals, not "today" — the interview count comes
 * from the persisted sessions table. Quiz/drill/code run entirely on SessionStore
 * (ephemeral, no persisted row per attempt — see sessionstore/package-info.java), so
 * their usage is not queryable from the database today and is reported as 0 pending
 * a persisted usage-log table.
 */
public record AdminStatsResponse(
    long sessionsToday,
    long sessionsTotal,
    long activeUsersToday,
    double avgSessionScore,
    Map<String, Long> featureUsage,
    long aiCallsToday
) {}
