package com.interviewlab.proficiency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderException;
import com.interviewlab.ai.AIProviderFactory;
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
public class EnglishService {

    private final AIProviderFactory     aiProviderFactory;
    private final EnglishPromptBuilder  promptBuilder;
    private final EnglishProperties     englishProperties;
    private final ProficiencyRepository proficiencyRepository;
    private final ProficiencyProperties proficiencyProperties;
    private final ObjectMapper          objectMapper;

    @Transactional
    public EnglishFeedback analyze(String transcript, String context, UUID userId) {
        String prompt  = promptBuilder.buildAnalysisPrompt(transcript, context);
        AIOptions opts = new AIOptions(englishProperties.temperature(), englishProperties.maxTokens(), false);

        String jsonResponse = aiProviderFactory.getDefaultProvider().generateJson(prompt, opts);

        EnglishFeedback feedback = parseFeedback(jsonResponse);
        updateProficiency(userId, feedback.fluencyScore());

        log.info("English analysis complete: userId={} fluencyScore={}", userId, feedback.fluencyScore());
        return feedback;
    }

    private EnglishFeedback parseFeedback(String jsonResponse) {
        try {
            int start = jsonResponse.indexOf('{');
            int end   = jsonResponse.lastIndexOf('}');
            if (start == -1 || end == -1 || end < start) {
                throw new IllegalArgumentException("No JSON object found in English analysis response");
            }
            JsonNode root = objectMapper.readTree(jsonResponse.substring(start, end + 1));
            return new EnglishFeedback(
                root.path("fluencyNote").asText(""),
                root.path("tenseFeedback").asText(""),
                root.path("fillerWordsDetected").asText(""),
                root.path("vocabularyNote").asText(""),
                root.path("confidenceNote").asText(""),
                root.path("improvedVersion").asText(""),
                root.path("fluencyScore").asInt(0)
            );
        } catch (AIProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse English analysis JSON: {}", ex.getMessage());
            throw new AIProviderException(
                ErrorCode.ENGLISH_ANALYSIS_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to parse English analysis from AI response"
            );
        }
    }

    private void updateProficiency(UUID userId, int fluencyScore) {
        Proficiency proficiency = proficiencyRepository.findByUserIdAndTopic(userId, "english")
            .orElseGet(() -> new Proficiency(
                userId, "english",
                proficiencyProperties.defaultScore(),
                proficiencyProperties.defaultSessionsCount()
            ));
        proficiency.setScore(fluencyScore);
        proficiency.setSessionsCount(proficiency.getSessionsCount() + 1);
        proficiencyRepository.save(proficiency);
    }
}
