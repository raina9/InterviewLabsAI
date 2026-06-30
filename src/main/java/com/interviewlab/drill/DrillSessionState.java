package com.interviewlab.drill;

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
        this.sessionId          = sessionId;
        this.topic              = topic;
        this.mode               = mode;
        this.rapidQuestionLimit = rapidQuestionLimit;
        this.deepTurnLimit      = deepTurnLimit;
        this.rapidQuestions     = new ArrayList<>(rapidQuestions);
        this.history            = new ArrayList<>();
        this.currentQuestion    = firstQuestion;
        this.complete           = false;
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
