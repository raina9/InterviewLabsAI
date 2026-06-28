package com.interviewlab.agent;

import com.interviewlab.session.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Assembles MentorAgent prompts from question/answer context and AgentTool results.
 * System prompt from AgentProperties — never hardcoded here.
 * JSON schema embedded in prompt so Gemini returns parseable structured output.
 */
@RequiredArgsConstructor
@Component
public class MentorAgentPromptBuilder {

    private final AgentProperties agentProperties;

    public String buildFeedbackPrompt(String question, String candidateAnswer,
                                      Session session, Map<String, String> toolResults) {
        return agentProperties.mentorSystemPrompt() + "\n\n"
            + "Interview Type: "   + session.getInterviewType() + "\n"
            + "Target Role: "      + session.getTargetRole()    + "\n"
            + "Question: "         + question                   + "\n"
            + "Candidate Answer: " + candidateAnswer            + "\n\n"
            + buildToolContext(toolResults)
            + """
              Respond ONLY with a valid JSON object in exactly this format:
              {
                "feedbackGood":    "what the candidate did well",
                "feedbackImprove": "specific areas to strengthen",
                "refinedAnswer":   "the candidate's answer restructured for clarity",
                "modelAnswer":     "an ideal self-contained answer for this question",
                "psychologyNote":  "why an interviewer would react positively or negatively",
                "score":           7
              }
              score must be an integer from 0 to 10. No text before or after the JSON.""";
    }

    private String buildToolContext(Map<String, String> toolResults) {
        StringBuilder sb = new StringBuilder();
        toolResults.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                sb.append("[").append(key).append("]\n").append(value).append("\n\n");
            }
        });
        return sb.toString();
    }
}
