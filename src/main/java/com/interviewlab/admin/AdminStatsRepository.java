package com.interviewlab.admin;

import com.interviewlab.session.Session;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/**
 * Single query point for admin dashboard aggregates — every method is a COUNT/AVG
 * aggregate query, never a row fetch, so the endpoint stays O(1) response size
 * regardless of table growth (no N+1, no entity graph loaded into memory).
 *
 * Extends the bare {@link Repository} marker (not JpaRepository) — this interface
 * exposes no CRUD surface, only the aggregates declared below. The JPQL below
 * queries AnswerFeedback directly even though the declared entity type is Session;
 * JPQL entity names resolve against the whole persistence unit, not the repository's
 * generic type, so this is safe and avoids standing up a second trivial repository
 * interface just for one AVG query.
 */
public interface AdminStatsRepository extends Repository<Session, UUID> {

    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt >= :since")
    long countSessionsCreatedSince(@Param("since") Instant since);

    @Query("SELECT COUNT(s) FROM Session s")
    long countSessionsTotal();

    @Query("SELECT COUNT(DISTINCT s.userId) FROM Session s WHERE s.createdAt >= :since")
    long countDistinctUsersSince(@Param("since") Instant since);

    @Query("SELECT COALESCE(AVG(a.score), 0) FROM AnswerFeedback a")
    double averageAnswerScore();
}
