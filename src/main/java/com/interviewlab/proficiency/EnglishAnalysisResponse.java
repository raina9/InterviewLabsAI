package com.interviewlab.proficiency;

public record EnglishAnalysisResponse(
    EnglishFeedback feedback
) {
    public static EnglishAnalysisResponse from(EnglishFeedback feedback) {
        return new EnglishAnalysisResponse(feedback);
    }
}
