package com.interviewlab.auth;

/**
 * User role — stored as TEXT in users.role via @Enumerated(EnumType.STRING).
 * CANDIDATE: default for every OAuth-created and dev-mode-created user.
 * ADMIN: manually promoted via direct DB update (no self-service promotion endpoint) —
 * the seeded dev user (see V9__add_user_role.sql) is ADMIN so local dev has an admin
 * account without any extra setup.
 */
public enum Role {
    CANDIDATE,
    ADMIN
}
