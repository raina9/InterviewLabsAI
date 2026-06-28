package com.interviewlab.proficiency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Assembles the English analysis prompt from transcript, context, and configured system prompt.
 * System prompt from EnglishProperties — never hardcoded here.
 * JSON schema embedded in prompt so the provider returns parseable structured output.
 */
@RequiredArgsConstructor
@Component
public class EnglishPromptBuilder {

    private final EnglishProperties englishProperties;

    public String buildAnalysisPrompt(String transcript, String context) {
        String ctx = (context != null && !context.isBlank()) ? context : "interview";
        return englishProperties.systemPrompt() + "\n\n"
            + "Context: " + ctx + "\n"
            + "Transcript: " + transcript + "\n\n"
            + """
              Respond ONLY with a valid JSON object in exactly this format:
              {
                "fluencyNote":         "overall assessment of fluency and naturalness",
                "tenseFeedback":       "grammar/tense errors found, or 'No errors detected'",
                "fillerWordsDetected": "comma-separated list of filler words e.g. 'um, uh, like', or 'None'",
                "vocabularyNote":      "assessment of vocabulary range and word choice",
                "confidenceNote":      "assessment of confidence from phrasing and sentence structure",
                "improvedVersion":     "the transcript rewritten with all corrections applied",
                "fluencyScore":        7
              }
              fluencyScore must be an integer from 0 to 10. No text before or after the JSON.""";
    }
}
