package com.interviewlab.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.agent.tools.AgentContext;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderException;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.session.Session;
import com.interviewlab.session.SessionException;
import com.interviewlab.session.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Mentor loop agent — answer evaluation ONLY.
 * Generates structured JSON feedback, then parses it into MentorFeedback.
 * Does NOT generate questions or manage session flow (InterviewAgent responsibility).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MentorAgent {

    private final AgentToolChain          agentToolChain;
    private final MentorAgentPromptBuilder promptBuilder;
    private final AIProviderFactory       aiProviderFactory;
    private final SessionRepository       sessionRepository;
    private final ObjectMapper            objectMapper;
    private final AiProperties            aiProperties;

    public MentorFeedback analyze(UUID sessionId, UUID messageId, String question, String candidateAnswer) {
        Session session = findSessionOrThrow(sessionId);

        AgentContext ctx = new AgentContext(session.getUserId(), sessionId, messageId, candidateAnswer);
        Map<String, String> toolResults = agentToolChain.execute(ctx);

        String prompt       = promptBuilder.buildFeedbackPrompt(question, candidateAnswer, session, toolResults);
        String jsonResponse = aiProviderFactory.getDefaultProvider().generateJson(prompt, feedbackOptions());

        MentorFeedback feedback = parseFeedback(jsonResponse);
        log.info("MentorAgent scored: sessionId={} messageId={} score={}", sessionId, messageId, feedback.score());
        return feedback;
    }

    private AIOptions feedbackOptions() {
        AiProperties.OptionsConfig opts = aiProperties.options();
        return new AIOptions(opts.feedbackTemperature(), opts.feedbackMaxTokens(), false);
    }

    private MentorFeedback parseFeedback(String jsonResponse) {
        try {
            int start = jsonResponse.indexOf('{');
            int end   = jsonResponse.lastIndexOf('}');
            if (start == -1 || end == -1 || end < start) {
                throw new IllegalArgumentException("No JSON object found in mentor response");
            }
            JsonNode root = objectMapper.readTree(jsonResponse.substring(start, end + 1));
            return new MentorFeedback(
                root.path("feedbackGood").asText(""),
                root.path("feedbackImprove").asText(""),
                root.path("refinedAnswer").asText(""),
                root.path("modelAnswer").asText(""),
                root.path("psychologyNote").asText(""),
                root.path("score").asInt(0)
            );
        } catch (AIProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse mentor feedback JSON: {}", ex.getMessage());
            throw new AIProviderException(
                ErrorCode.AI_RESPONSE_PARSE_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to parse mentor feedback from AI response"
            );
        }
    }

    private Session findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionException(
                ErrorCode.SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Session " + sessionId + " not found"
            ));
    }
}
