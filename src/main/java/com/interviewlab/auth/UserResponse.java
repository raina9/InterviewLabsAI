package com.interviewlab.auth;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String name,
    String picture
) {
    public static UserResponse from(AuthenticatedUser principal) {
        return new UserResponse(principal.id(), principal.email(), principal.name(), principal.picture());
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getPicture());
    }
}
