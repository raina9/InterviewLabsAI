package com.interviewlab.admin;

import com.interviewlab.ai.AiBudgetGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AdminStatsService {

    private final AdminStatsRepository adminStatsRepository;
    private final AiBudgetGuard        aiBudgetGuard;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();

        long sessionsToday    = adminStatsRepository.countSessionsCreatedSince(startOfToday);
        long sessionsTotal    = adminStatsRepository.countSessionsTotal();
        long activeUsersToday = adminStatsRepository.countDistinctUsersSince(startOfToday);
        double avgScore       = adminStatsRepository.averageAnswerScore();
        long aiCallsToday     = aiBudgetGuard.todaysCallCount();

        Map<String, Long> featureUsage = new LinkedHashMap<>();
        featureUsage.put("interview", sessionsTotal);
        // quiz/drill/code are SessionStore-ephemeral — see AdminStatsResponse javadoc.
        featureUsage.put("quiz", 0L);
        featureUsage.put("drill", 0L);
        featureUsage.put("code", 0L);

        return new AdminStatsResponse(
            sessionsToday, sessionsTotal, activeUsersToday, avgScore, featureUsage, aiCallsToday
        );
    }
}
