package com.interviewlab.agent;

public record MentorFeedback(
    String feedbackGood,
    String feedbackImprove,
    String refinedAnswer,
    String modelAnswer,
    String psychologyNote,
    int    score
) {}
