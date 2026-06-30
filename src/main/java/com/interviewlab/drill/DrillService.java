package com.interviewlab.drill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.auth.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class DrillService {

    private static final AIOptions GENERATE_OPTIONS  = new AIOptions(0.6f, 1200, false);
    private static final AIOptions EVALUATE_OPTIONS  = new AIOptions(0.3f, 500,  false);
    private static final AIOptions SOCRATIC_OPTIONS  = new AIOptions(0.5f, 600,  false);

    private final AIProviderFactory aiProviderFactory;
    private final ObjectMapper      objectMapper;

    private final Map<UUID, DrillSessionState> sessions = new ConcurrentHashMap<>();

    public DrillSession startDrill(DrillRequest request) {
        List<String> rapidQuestions = new ArrayList<>();
        String firstQuestion;

        if (request.mode() == DrillMode.RAPID) {
            rapidQuestions = generateRapidQuestions(request.topic());
            firstQuestion  = rapidQuestions.isEmpty() ? "Explain " + request.topic() + " in simple terms." : rapidQuestions.get(0);
        } else {
            firstQuestion = generateOpeningDeepQuestion(request.topic());
        }

        UUID sessionId = UUID.randomUUID();
        DrillSessionState state = new DrillSessionState(
            sessionId, request.topic(), request.mode(), rapidQuestions, firstQuestion
        );
        sessions.put(sessionId, state);
        log.info("Drill started: sessionId={} topic={} mode={}", sessionId, request.topic(), request.mode());

        return toSession(state);
    }

    public DrillQuestionResponse nextQuestion(UUID sessionId, DrillAnswerRequest request) {
        DrillSessionState state = findSessionOrThrow(sessionId);
        if (state.isComplete()) {
            throw new DrillException(
                ErrorCode.DRILL_ALREADY_COMPLETED,
                HttpStatus.CONFLICT,
                "Drill session " + sessionId + " is already complete. Retrieve the summary instead."
            );
        }

        // Evaluate the submitted answer to the current question
        int    score    = evaluateAnswer(state.getCurrentQuestion(), request.answer(), state.getTopic());
        String feedback = generateFeedback(state.getCurrentQuestion(), request.answer(), state.getTopic());
        state.recordTurn(request.answer(), score);
        log.debug("Drill turn: sessionId={} score={}", sessionId, score);

        // Generate next question
        String nextQuestion;
        if (state.getMode() == DrillMode.RAPID) {
            nextQuestion = state.nextRapidQuestion();
        } else {
            nextQuestion = state.isComplete() ? null : generateDeepFollowUp(
                state.getTopic(), state.getCurrentQuestion(), request.answer()
            );
        }

        state.advanceTo(nextQuestion);

        return new DrillQuestionResponse(
            nextQuestion,
            state.getHistory().size() + 1,
            state.isComplete(),
            feedback,
            score
        );
    }

    public DrillSummary getSummary(UUID sessionId) {
        DrillSessionState state = findSessionOrThrow(sessionId);
        List<DrillSessionState.Turn> history = state.getHistory();

        double avg = history.stream().mapToInt(DrillSessionState.Turn::score).average().orElse(0);

        List<String> weakSpots = history.stream()
            .filter(t -> t.score() < 5)
            .map(DrillSessionState.Turn::question)
            .toList();

        List<String> strongPoints = history.stream()
            .filter(t -> t.score() >= 7)
            .map(DrillSessionState.Turn::question)
            .toList();

        log.info("Drill summary: sessionId={} answered={} avg={}", sessionId, history.size(), avg);
        sessions.remove(sessionId);
        return new DrillSummary(
            state.getTopic(), state.getMode(),
            history.size(), avg, weakSpots, strongPoints
        );
    }

    private DrillSession toSession(DrillSessionState state) {
        return new DrillSession(
            state.getSessionId(), state.getTopic(), state.getMode(),
            state.getCurrentQuestion(), state.getHistory().size(), state.isComplete()
        );
    }

    private DrillSessionState findSessionOrThrow(UUID sessionId) {
        DrillSessionState state = sessions.get(sessionId);
        if (state == null) {
            throw new DrillException(
                ErrorCode.DRILL_SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Drill session " + sessionId + " not found. It may have expired or not been started."
            );
        }
        return state;
    }

    private List<String> generateRapidQuestions(String topic) {
        String prompt = """
            Generate exactly 10 short, focused Q&A questions about "%s" for rapid-fire drilling.
            Each question should be answerable in 2-3 sentences. Return ONLY valid JSON:
            {"questions": ["Question 1?", "Question 2?", ..., "Question 10?"]}
            """.formatted(topic);
        String raw = aiProviderFactory.getDefaultProvider().generateJson(prompt, GENERATE_OPTIONS);
        try {
            String json = extractJson(raw);
            Map<String, List<String>> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            return parsed.getOrDefault("questions", List.of());
        } catch (Exception e) {
            log.warn("Failed to parse rapid questions, using fallback: {}", e.getMessage());
            return List.of("Explain " + topic + " in your own words.");
        }
    }

    private String generateOpeningDeepQuestion(String topic) {
        String prompt = "Generate one open-ended interview question about \"%s\" that invites a deep, detailed answer. Return just the question, no additional text.".formatted(topic);
        return aiProviderFactory.getDefaultProvider().generate(prompt, AIOptions.defaults()).trim();
    }

    private String generateDeepFollowUp(String topic, String prevQuestion, String prevAnswer) {
        String prompt = """
            You are conducting a Socratic technical interview about "%s".
            Previous question: %s
            Candidate's answer: %s
            Generate ONE sharp follow-up question that probes deeper or challenges an assumption in the answer.
            Return just the question, no additional text.
            """.formatted(topic, prevQuestion, prevAnswer);
        return aiProviderFactory.getDefaultProvider().generate(prompt, SOCRATIC_OPTIONS).trim();
    }

    private int evaluateAnswer(String question, String answer, String topic) {
        String prompt = """
            Evaluate this answer on a scale of 1-10 for a technical question about "%s".
            Question: %s
            Answer: %s
            Return ONLY valid JSON: {"score": 7}
            Score guide: 1-3=incorrect/very weak, 4-6=partial, 7-8=good, 9-10=excellent.
            """.formatted(topic, question, answer);
        String raw = aiProviderFactory.getDefaultProvider().generateJson(prompt, EVALUATE_OPTIONS);
        try {
            String json = extractJson(raw);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            Object scoreVal = parsed.get("score");
            return scoreVal instanceof Number n ? n.intValue() : 5;
        } catch (Exception e) {
            log.warn("Failed to parse drill score, defaulting to 5: {}", e.getMessage());
            return 5;
        }
    }

    private String generateFeedback(String question, String answer, String topic) {
        String prompt = """
            Give brief feedback (1-2 sentences) on this answer to a technical question about "%s".
            Question: %s
            Answer: %s
            Be specific and actionable. Return just the feedback text, no JSON.
            """.formatted(topic, question, answer);
        return aiProviderFactory.getDefaultProvider().generate(prompt, EVALUATE_OPTIONS).trim();
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) return raw;
        return raw.substring(start, end + 1);
    }
}
