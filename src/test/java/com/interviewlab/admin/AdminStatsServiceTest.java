package com.interviewlab.admin;

import com.interviewlab.ai.AiBudgetGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock AdminStatsRepository adminStatsRepository;
    @Mock AiBudgetGuard        aiBudgetGuard;
    @InjectMocks AdminStatsService adminStatsService;

    @Test
    void getStats_assemblesResponseFromRepositoryAndBudgetGuard() {
        when(adminStatsRepository.countSessionsCreatedSince(any(Instant.class))).thenReturn(5L);
        when(adminStatsRepository.countSessionsTotal()).thenReturn(42L);
        when(adminStatsRepository.countDistinctUsersSince(any(Instant.class))).thenReturn(3L);
        when(adminStatsRepository.averageAnswerScore()).thenReturn(7.5);
        when(aiBudgetGuard.todaysCallCount()).thenReturn(12L);

        AdminStatsResponse result = adminStatsService.getStats();

        assertThat(result.sessionsToday()).isEqualTo(5L);
        assertThat(result.sessionsTotal()).isEqualTo(42L);
        assertThat(result.activeUsersToday()).isEqualTo(3L);
        assertThat(result.avgSessionScore()).isEqualTo(7.5);
        assertThat(result.aiCallsToday()).isEqualTo(12L);
        assertThat(result.featureUsage())
            .containsEntry("interview", 42L)
            .containsEntry("quiz", 0L)
            .containsEntry("drill", 0L)
            .containsEntry("code", 0L);
    }
}
