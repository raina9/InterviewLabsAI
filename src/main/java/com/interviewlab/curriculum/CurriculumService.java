package com.interviewlab.curriculum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.assessment.AssessmentReport;
import com.interviewlab.assessment.AssessmentService;
import com.interviewlab.auth.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CurriculumService {

    // Lower temperature for deterministic curriculum order; more tokens for multi-item JSON.
    private static final AIOptions CURRICULUM_OPTIONS = new AIOptions(0.4f, 1500, false);

    private final AssessmentService    assessmentService;
    private final CurriculumPromptBuilder promptBuilder;
    private final AIProviderFactory    aiProviderFactory;
    private final ObjectMapper         objectMapper;

    @Transactional(readOnly = true)
    public CurriculumPlan generatePlan(UUID userId) {
        AssessmentReport report = assessmentService.generateReport(userId);
        String prompt           = promptBuilder.buildCurriculumPrompt(report);
        String rawJson          = aiProviderFactory.getDefaultProvider().generateJson(prompt, CURRICULUM_OPTIONS);

        try {
            String json = extractJson(rawJson);
            CurriculumPlan plan = objectMapper.readValue(json, CurriculumPlan.class);
            log.info("Curriculum plan generated: userId={} items={}", userId, plan.items().size());
            return plan;
        } catch (Exception e) {
            log.error("Failed to parse curriculum JSON for userId={}: {}", userId, e.getMessage());
            throw new CurriculumException(
                ErrorCode.CURRICULUM_GENERATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate curriculum. Please retry your request."
            );
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new CurriculumException(
                ErrorCode.CURRICULUM_GENERATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate curriculum. Please retry your request."
            );
        }
        return raw.substring(start, end + 1);
    }
}
