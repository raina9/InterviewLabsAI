package com.interviewlab.interview;

public record InterviewTurnResponse(
    String                agentResponse,
    boolean               sessionComplete,
    MentorFeedbackResponse mentorFeedback
) {}
