package com.interviewlab.agent;

import java.util.UUID;

public record InterviewTurnResult(
    String  agentResponse,
    boolean shouldMoveToNextQuestion,
    int     currentQuestionNumber,
    UUID    candidateMessageId
) {}
