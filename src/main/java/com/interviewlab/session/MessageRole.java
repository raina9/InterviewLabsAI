package com.interviewlab.session;

/**
 * Message author role within a session transcript.
 * Stored as TEXT via @Enumerated(EnumType.STRING).
 */
public enum MessageRole {
    INTERVIEWER,
    CANDIDATE,
    MENTOR
}
