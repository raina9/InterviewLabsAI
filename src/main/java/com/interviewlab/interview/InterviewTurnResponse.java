package com.interviewlab.interview;

import com.interviewlab.psychology.PsychologyInsight;

public record InterviewTurnResponse(
    String                agentResponse,
    boolean               sessionComplete,
    MentorFeedbackResponse mentorFeedback,
    PsychologyInsight     psychologyNudge
) {}
