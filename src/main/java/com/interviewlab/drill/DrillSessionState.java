package com.interviewlab.drill;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DrillSessionState {

    public record Turn(String question, String answer, int score) {}

    private final UUID      sessionId;
    private final String    topic;
    private final DrillMode mode;
    private final int       rapidQuestionLimit;
    private final int       deepTurnLimit;
    private final List<String> rapidQuestions;
    private final List<Turn>   history;
    private String             currentQuestion;
    private boolean            complete;

    public DrillSessionState(UUID sessionId, String topic, DrillMode mode,
                             List<String> rapidQuestions, String firstQuestion,
                             int rapidQuestionLimit, int deepTurnLimit) {
        this(sessionId, topic, mode, rapidQuestionLimit, deepTurnLimit,
            new ArrayList<>(rapidQuestions), new ArrayList<>(), firstQuestion, false);
    }

    // Full-state constructor — required for GenericJackson2JsonRedisSerializer to
    // round-trip mutated state (history/currentQuestion/complete) through RedisSessionStore.
    @JsonCreator
    public DrillSessionState(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("topic") String topic,
            @JsonProperty("mode") DrillMode mode,
            @JsonProperty("rapidQuestionLimit") int rapidQuestionLimit,
            @JsonProperty("deepTurnLimit") int deepTurnLimit,
            @JsonProperty("rapidQuestions") List<String> rapidQuestions,
            @JsonProperty("history") List<Turn> history,
            @JsonProperty("currentQuestion") String currentQuestion,
            @JsonProperty("complete") boolean complete) {
        this.sessionId          = sessionId;
        this.topic              = topic;
        this.mode               = mode;
        this.rapidQuestionLimit = rapidQuestionLimit;
        this.deepTurnLimit      = deepTurnLimit;
        this.rapidQuestions     = new ArrayList<>(rapidQuestions);
        this.history            = new ArrayList<>(history);
        this.currentQuestion    = currentQuestion;
        this.complete           = complete;
    }

    public UUID    getSessionId()       { return sessionId; }
    public String  getTopic()           { return topic; }
    public DrillMode getMode()          { return mode; }
    public List<Turn> getHistory()      { return history; }
    public String  getCurrentQuestion() { return currentQuestion; }
    public boolean isComplete()         { return complete; }

    public void recordTurn(String answer, int score) {
        history.add(new Turn(currentQuestion, answer, score));
    }

    public void advanceTo(String nextQuestion) {
        currentQuestion = nextQuestion;
        int limit = mode == DrillMode.RAPID ? rapidQuestionLimit : deepTurnLimit;
        if (nextQuestion == null || history.size() >= limit) complete = true;
    }

    /** Next pre-generated RAPID question after the current index, or null if exhausted. */
    public String nextRapidQuestion() {
        int nextIdx = history.size();
        return nextIdx < rapidQuestions.size() ? rapidQuestions.get(nextIdx) : null;
    }
}
