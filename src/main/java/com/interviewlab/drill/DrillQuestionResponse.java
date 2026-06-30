package com.interviewlab.drill;

public record DrillQuestionResponse(
    String  question,
    int     questionNumber,
    boolean sessionComplete,
    String  feedback,       // feedback on the previous answer (null on first question)
    int     previousScore   // 0 when no previous answer
) {}
