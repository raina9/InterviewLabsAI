package com.interviewlab.quiz;

import java.util.Map;

public record QuizResult(
    int                 totalQuestions,
    int                 correctAnswers,
    int                 scorePercent,
    Map<String, Integer> topicBreakdown
) {}
