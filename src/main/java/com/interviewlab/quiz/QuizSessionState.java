package com.interviewlab.quiz;

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
        this.sessionId    = sessionId;
        this.topic        = topic;
        this.difficulty   = difficulty;
        this.questions    = questions;
        this.currentIndex = 0;
        this.score        = 0;
        this.complete     = false;
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
