package com.interviewlab.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Account", description = "Authenticated user's own account management")
public class UserController {

    private final UserAccountService userAccountService;

    @DeleteMapping
    @Operation(
        summary = "Permanently delete the authenticated user's account and all associated data",
        description = "GDPR-style right to erasure. Deletes answer feedback, messages, proficiency, " +
            "sessions, profile, and the user record itself. Irreversible.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Account and all data deleted"),
            @ApiResponse(responseCode = "401", description = "No valid authentication present")
        }
    )
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_MISSING);
        }
        userAccountService.deleteAccount(principal.id());
        log.info("Account deletion requested via API: userId={}", principal.id());
        return ResponseEntity.noContent().build();
    }
}
