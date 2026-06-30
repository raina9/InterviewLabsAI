package com.interviewlab.auth;

public enum ErrorCode {

    AUTH_TOKEN_MISSING("Authentication token is missing. Please log in to continue."),
    AUTH_TOKEN_INVALID("Authentication token is invalid or has been tampered with. Please log in again."),
    AUTH_TOKEN_EXPIRED("Authentication token has expired. Please log in again."),
    USER_NOT_FOUND("User account not found."),

    SESSION_NOT_FOUND("Session not found."),
    SESSION_NOT_ACTIVE("Session is not in ACTIVE state and cannot accept new messages."),
    SESSION_ALREADY_COMPLETED("Session has already been completed."),
    PROFILE_NOT_FOUND("User profile not found. Please complete your profile setup."),
    FEEDBACK_NOT_FOUND("Answer feedback record not found."),
    PROFICIENCY_NOT_FOUND("Proficiency record not found for the specified topic."),

    SESSION_ACCESS_DENIED("You do not have access to this session."),
    PROFILE_UPDATE_FAILED("Profile update failed. Please try again."),

    INTERVIEW_SESSION_NOT_ACTIVE("Session is not in ACTIVE state. Only ACTIVE sessions can accept interview turns."),
    INTERVIEW_ALREADY_STARTED("This interview session has already been started. Each session can only be initiated once."),

    AI_SERVICE_UNAVAILABLE("The AI service is temporarily unavailable. Please try again shortly."),
    AI_RESPONSE_PARSE_FAILED("The AI response could not be parsed. Please retry your request."),
    RATE_LIMIT_EXCEEDED("Request rate limit exceeded. Please wait before retrying."),
    AI_PROVIDER_NOT_IMPLEMENTED("This AI provider is not yet available."),

    ENGLISH_ANALYSIS_FAILED("English proficiency analysis failed. Please retry your request."),

    ASSESSMENT_PROFILE_NOT_FOUND("User profile not found. Complete your profile with tech stack before starting assessment."),
    ASSESSMENT_NOT_FOUND("No assessment data found. Complete a self-assessment first."),

    CURRICULUM_GENERATION_FAILED("Failed to generate curriculum. Please retry your request."),

    QUIZ_SESSION_NOT_FOUND("Quiz session not found. It may have expired or not been started."),
    QUIZ_ALREADY_COMPLETED("This quiz session is already complete. Retrieve the result instead."),
    QUIZ_NOT_YET_COMPLETE("Quiz session is not yet complete. Answer all questions before retrieving the result."),
    QUIZ_GENERATION_FAILED("Failed to generate quiz questions. Please retry your request."),

    CODE_CHALLENGE_NOT_FOUND("Code challenge not found. Generate a new challenge."),
    CODE_CHALLENGE_GENERATION_FAILED("Failed to generate code challenge. Please retry your request."),
    CODE_EVALUATION_FAILED("Failed to evaluate code submission. Please retry your request."),

    DRILL_SESSION_NOT_FOUND("Drill session not found. It may have expired or not been started."),
    DRILL_ALREADY_COMPLETED("This drill session is already complete. Retrieve the summary instead."),
    DRILL_GENERATION_FAILED("Failed to generate drill content. Please retry your request.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
