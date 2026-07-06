package com.interviewlab.auth;

import java.util.UUID;

/**
 * Principal stored in SecurityContext after JWT validation.
 * Reconstructed from JWT claims — no DB hit per request (performance: O(1) per request).
 * role is embedded in the JWT at sign time (JwtService) so this stays a pure claims
 * reconstruction with no DB lookup, same as every other field here.
 */
public record AuthenticatedUser(
    UUID id,
    String email,
    String name,
    String picture,
    Role role
) {
    // Back-compat constructor — defaults role to CANDIDATE. Existing callers that only
    // care about identity (not authorization) are unaffected by the role addition.
    public AuthenticatedUser(UUID id, String email, String name, String picture) {
        this(id, email, name, picture, Role.CANDIDATE);
    }
}
