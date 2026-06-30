package com.interviewlab.drill;

import java.util.UUID;

public record DrillSession(
    UUID      sessionId,
    String    topic,
    DrillMode mode,
    String    currentQuestion,
    int       questionsAnswered,
    boolean   complete
) {}
