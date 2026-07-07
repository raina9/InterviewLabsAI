package com.interviewlab.admin;

import com.interviewlab.feedback.AnswerFeedback;
import com.interviewlab.session.InterviewType;
import com.interviewlab.session.Session;
import com.interviewlab.session.SessionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AdminStatsRepositoryTest {

    @Autowired private AdminStatsRepository adminStatsRepository;
    @Autowired private TestEntityManager    entityManager;

    @Test
    void countSessionsCreatedSince_countsOnlySessionsAtOrAfterCutoff() {
        persistSessionForUser(UUID.randomUUID());
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

        assertThat(adminStatsRepository.countSessionsCreatedSince(cutoff)).isEqualTo(1);
    }

    @Test
    void countSessionsTotal_countsAllSessionsRegardlessOfDate() {
        persistSessionForUser(UUID.randomUUID());
        persistSessionForUser(UUID.randomUUID());

        assertThat(adminStatsRepository.countSessionsTotal()).isEqualTo(2);
    }

    @Test
    void countDistinctUsersSince_deduplicatesSameUserAcrossMultipleSessions() {
        UUID sharedUser = UUID.randomUUID();
        persistSessionForUser(sharedUser);
        persistSessionForUser(sharedUser);
        persistSessionForUser(UUID.randomUUID());
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

        assertThat(adminStatsRepository.countDistinctUsersSince(cutoff)).isEqualTo(2);
    }

    @Test
    void averageAnswerScore_returnsZero_whenNoFeedbackExists() {
        assertThat(adminStatsRepository.averageAnswerScore()).isEqualTo(0.0);
    }

    @Test
    void averageAnswerScore_averagesAcrossAllFeedbackRows() {
        persistFeedback(8);
        persistFeedback(4);

        assertThat(adminStatsRepository.averageAnswerScore()).isEqualTo(6.0);
    }

    private void persistSessionForUser(UUID userId) {
        entityManager.persistAndFlush(
            new Session(userId, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE)
        );
    }

    private void persistFeedback(int score) {
        entityManager.persistAndFlush(new AnswerFeedback(
            UUID.randomUUID(), UUID.randomUUID(), "Q", "A", null, null,
            score, "good", "improve", "note"
        ));
    }
}
