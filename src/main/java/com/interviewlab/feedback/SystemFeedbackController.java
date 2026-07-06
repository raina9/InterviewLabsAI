package com.interviewlab.feedback;

import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only system feedback management (G2 fix — see docs/forgekit/TODO.md).
 * Role check is an explicit in-controller check, not @PreAuthorize — this is the only
 * role-gated endpoint in the app so far; adding @EnableMethodSecurity + AOP proxying for
 * a single check point is more moving parts than the check itself (see ADR-011 project
 * note in session notes for the tradeoff).
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/system-feedback")
@Tag(name = "Admin", description = "Administrator-only endpoints")
public class SystemFeedbackController {

    private final SystemFeedbackService systemFeedbackService;

    @Operation(
        summary = "Toggle whether a system feedback submission has been applied",
        description = "ADMIN role required. Returns 403 FORBIDDEN_ADMIN_ONLY for any other caller."
    )
    @PatchMapping("/{id}/applied")
    public ResponseEntity<Void> toggleApplied(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestBody ToggleAppliedRequest request) {
        requireAdmin(principal);
        systemFeedbackService.setApplied(id, request.applied());
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(AuthenticatedUser principal) {
        if (principal == null || principal.role() != Role.ADMIN) {
            log.warn("Non-admin access denied: principal={}", principal == null ? "anonymous" : principal.email());
            throw new FeedbackException(
                ErrorCode.FORBIDDEN_ADMIN_ONLY,
                HttpStatus.FORBIDDEN,
                "This action requires administrator privileges."
            );
        }
    }
}
