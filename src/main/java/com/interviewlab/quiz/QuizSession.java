package com.interviewlab.quiz;

import java.util.List;
import java.util.UUID;

public record QuizSession(
    UUID         sessionId,
    String       topic,
    String       difficulty,
    int          totalQuestions,
    int          currentIndex,
    int          score,
    String       currentQuestion,
    List<String> currentOptions
) {}
