package com.interviewlab.auth;

import java.util.UUID;

/**
 * Principal stored in SecurityContext after JWT validation.
 * Reconstructed from JWT claims — no DB hit per request (performance: O(1) per request).
 * V2 note: if role-based access is added, include roles in JWT claims and here.
 */
public record AuthenticatedUser(
    UUID id,
    String email,
    String name,
    String picture
) {}
