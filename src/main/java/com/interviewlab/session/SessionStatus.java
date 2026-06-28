package com.interviewlab.session;

/**
 * Session lifecycle states.
 * State machine: ACTIVE → COMPLETED | ABANDONED
 * Stored as TEXT via @Enumerated(EnumType.STRING).
 */
public enum SessionStatus {
    ACTIVE,
    COMPLETED,
    ABANDONED
}
