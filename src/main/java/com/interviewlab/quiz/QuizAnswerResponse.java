package com.interviewlab.quiz;

import java.util.List;

public record QuizAnswerResponse(
    boolean      correct,
    String       explanation,
    String       correctAnswer,
    int          score,
    int          totalAnswered,
    boolean      sessionComplete,
    // next question — null when sessionComplete
    String       nextQuestion,
    List<String> nextOptions
) {}
