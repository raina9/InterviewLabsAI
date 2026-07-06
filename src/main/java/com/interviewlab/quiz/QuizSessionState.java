package com.interviewlab.quiz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class QuizSessionState {

    private final UUID             sessionId;
    private final String           topic;
    private final String           difficulty;
    private final List<QuizQuestion> questions;
    private int                    currentIndex;
    private int                    score;
    private boolean                complete;

    public QuizSessionState(UUID sessionId, String topic, String difficulty, List<QuizQuestion> questions) {
        this(sessionId, topic, difficulty, questions, 0, 0, false);
    }

    // Full-state constructor — required for GenericJackson2JsonRedisSerializer to
    // round-trip mutated state (currentIndex/score/complete) through RedisSessionStore.
    @JsonCreator
    public QuizSessionState(
            @JsonProperty("sessionId") UUID sessionId,
            @JsonProperty("topic") String topic,
            @JsonProperty("difficulty") String difficulty,
            @JsonProperty("questions") List<QuizQuestion> questions,
            @JsonProperty("currentIndex") int currentIndex,
            @JsonProperty("score") int score,
            @JsonProperty("complete") boolean complete) {
        this.sessionId    = sessionId;
        this.topic        = topic;
        this.difficulty   = difficulty;
        this.questions    = questions;
        this.currentIndex = currentIndex;
        this.score        = score;
        this.complete     = complete;
    }

    public UUID             getSessionId()    { return sessionId; }
    public String           getTopic()        { return topic; }
    public String           getDifficulty()   { return difficulty; }
    public List<QuizQuestion> getQuestions()  { return questions; }
    public int              getCurrentIndex() { return currentIndex; }
    public int              getScore()        { return score; }
    public boolean          isComplete()      { return complete; }

    public QuizQuestion currentQuestion() {
        return complete || currentIndex >= questions.size() ? null : questions.get(currentIndex);
    }

    public void advance(boolean correct) {
        if (correct) score++;
        currentIndex++;
        if (currentIndex >= questions.size()) complete = true;
    }
}
