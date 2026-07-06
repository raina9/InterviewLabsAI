package com.interviewlab.quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.sessionstore.SessionStore;
import com.interviewlab.sessionstore.SessionTtlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class QuizService {

    private static final String KEY_PREFIX = "quiz:";

    private final AIProviderFactory     aiProviderFactory;
    private final ObjectMapper          objectMapper;
    private final AiProperties          aiProperties;
    private final SessionStore          sessionStore;
    private final SessionTtlProperties  sessionTtlProperties;

    public QuizSession startQuiz(StartQuizRequest request) {
        List<QuizQuestion> questions = generateQuestions(request.topic(), request.difficulty(), request.questionCount());
        UUID sessionId = UUID.randomUUID();
        QuizSessionState state = new QuizSessionState(sessionId, request.topic(), request.difficulty(), questions);
        sessionStore.put(key(sessionId), state, sessionTtlProperties.quiz());
        log.info("Quiz started: sessionId={} topic={} count={}", sessionId, request.topic(), questions.size());

        QuizQuestion first = state.currentQuestion();
        return new QuizSession(
            sessionId, request.topic(), request.difficulty(),
            questions.size(), 0, 0,
            first.question(), first.options()
        );
    }

    public QuizAnswerResponse submitAnswer(UUID sessionId, String answer) {
        QuizSessionState state = findSessionOrThrow(sessionId);
        if (state.isComplete()) {
            throw new QuizException(
                ErrorCode.QUIZ_ALREADY_COMPLETED,
                HttpStatus.CONFLICT,
                "Quiz session " + sessionId + " is already complete. Retrieve the result instead."
            );
        }

        QuizQuestion current = state.currentQuestion();
        boolean correct = current.correctAnswer().equalsIgnoreCase(answer.trim());
        state.advance(correct);
        sessionStore.put(key(sessionId), state, sessionTtlProperties.quiz());
        log.debug("Quiz answer: sessionId={} correct={} score={}", sessionId, correct, state.getScore());

        QuizQuestion next = state.currentQuestion();
        return new QuizAnswerResponse(
            correct,
            current.explanation(),
            current.correctAnswer(),
            state.getScore(),
            state.getCurrentIndex(),
            state.isComplete(),
            next != null ? next.question() : null,
            next != null ? next.options()  : null
        );
    }

    public QuizResult getResult(UUID sessionId) {
        QuizSessionState state = findSessionOrThrow(sessionId);
        if (!state.isComplete()) {
            throw new QuizException(
                ErrorCode.QUIZ_NOT_YET_COMPLETE,
                HttpStatus.CONFLICT,
                "Quiz session " + sessionId + " is not yet complete. Answer all questions before retrieving the result."
            );
        }
        int total   = state.getQuestions().size();
        int correct = state.getScore();
        int percent = total > 0 ? (correct * 100 / total) : 0;
        log.info("Quiz result: sessionId={} correct={}/{} score={}%", sessionId, correct, total, percent);
        sessionStore.delete(key(sessionId));
        return new QuizResult(total, correct, percent, Map.of(state.getTopic(), correct));
    }

    private QuizSessionState findSessionOrThrow(UUID sessionId) {
        QuizSessionState state = sessionStore.get(key(sessionId), QuizSessionState.class);
        if (state == null) {
            throw new QuizException(
                ErrorCode.QUIZ_SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Quiz session " + sessionId + " not found. It may have expired or not been started."
            );
        }
        return state;
    }

    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private List<QuizQuestion> generateQuestions(String topic, String difficulty, int count) {
        String prompt = buildQuizPrompt(topic, difficulty, count);
        AIOptions opts = quizOptions();
        String raw    = aiProviderFactory.getDefaultProvider().generateJson(prompt, opts);
        try {
            String json = extractJson(raw);
            Map<String, List<QuizQuestion>> parsed = objectMapper.readValue(
                json, new TypeReference<Map<String, List<QuizQuestion>>>() {}
            );
            List<QuizQuestion> questions = parsed.get("questions");
            if (questions == null || questions.isEmpty()) {
                throw new IllegalStateException("AI returned empty questions list");
            }
            return questions;
        } catch (Exception e) {
            log.error("Failed to parse quiz questions for topic={}: {}", topic, e.getMessage());
            throw new QuizException(
                ErrorCode.QUIZ_GENERATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate quiz questions. Please retry your request."
            );
        }
    }

    private AIOptions quizOptions() {
        AiProperties.QuizOptions q = aiProperties.quiz();
        return new AIOptions(q.temperature(), q.maxTokens(), false);
    }

    private String buildQuizPrompt(String topic, String difficulty, int count) {
        return """
            You are a technical quiz generator for software engineers.
            Generate exactly %d multiple-choice questions about "%s" at %s difficulty level.
            Return ONLY valid JSON in this exact format — no markdown, no explanation:
            {
              "questions": [
                {
                  "question": "...",
                  "options": ["option A", "option B", "option C", "option D"],
                  "correctAnswer": "option A",
                  "explanation": "..."
                }
              ]
            }
            Rules:
            - Exactly 4 options per question.
            - correctAnswer must match one of the options exactly.
            - Difficulty levels: easy=conceptual, medium=applied, hard=deep-internals.
            - Return ONLY valid JSON, nothing else.
            """.formatted(count, topic, difficulty);
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new QuizException(
                ErrorCode.QUIZ_GENERATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate quiz questions. Please retry your request."
            );
        }
        return raw.substring(start, end + 1);
    }
}
