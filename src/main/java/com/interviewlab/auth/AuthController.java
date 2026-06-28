package com.interviewlab.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication — Google OAuth2 login and session management")
public class AuthController {

    @GetMapping("/me")
    @Operation(
        summary = "Get current authenticated user",
        description = "Returns the user profile from the JWT claims. Requires a valid JWT cookie.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authenticated user profile"),
            @ApiResponse(responseCode = "401", description = "No valid JWT cookie present")
        }
    )
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_MISSING);
        }
        return ResponseEntity.ok(UserResponse.from(principal));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Log out",
        description = "Clears the JWT httpOnly cookie. Frontend should redirect to login after this call.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Logged out — cookie cleared")
        }
    )
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie expired = new Cookie(JwtAuthFilter.JWT_COOKIE_NAME, "");
        expired.setHttpOnly(true);
        expired.setPath("/");
        expired.setMaxAge(0);
        response.addCookie(expired);
        log.debug("User logged out — JWT cookie cleared");
        return ResponseEntity.noContent().build();
    }
}
