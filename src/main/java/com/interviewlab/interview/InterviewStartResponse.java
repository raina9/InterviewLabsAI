package com.interviewlab.interview;

import java.util.UUID;

public record InterviewStartResponse(
    UUID   sessionId,
    String firstQuestion,
    int    totalQuestions
) {}
