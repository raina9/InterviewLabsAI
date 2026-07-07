package com.interviewlab.admin;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role check is an explicit in-controller check, not @PreAuthorize — same choice
 * and rationale as SystemFeedbackController (see that class's javadoc).
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/stats")
@Tag(name = "Admin", description = "Administrator-only endpoints")
public class AdminController {

    private final AdminStatsService adminStatsService;

    @Operation(
        summary = "Platform usage and health statistics",
        description = "ADMIN role required. Returns 403 FORBIDDEN_ADMIN_ONLY for any other caller."
    )
    @GetMapping
    public ResponseEntity<AdminStatsResponse> getStats(@AuthenticationPrincipal AuthenticatedUser principal) {
        requireAdmin(principal);
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    private void requireAdmin(AuthenticatedUser principal) {
        if (principal == null || principal.role() != Role.ADMIN) {
            log.warn("Non-admin access denied: principal={}", principal == null ? "anonymous" : principal.email());
            throw new AdminException(
                ErrorCode.FORBIDDEN_ADMIN_ONLY,
                HttpStatus.FORBIDDEN,
                "This action requires administrator privileges."
            );
        }
    }
}
