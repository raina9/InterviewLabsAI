package com.interviewlab.assessment;

import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/assessment")
@Tag(name = "Assessment", description = "Self-assessment intake and proficiency report")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "Start assessment — returns topic list derived from user tech stack")
    @PostMapping("/start")
    public ResponseEntity<AssessmentStartResponse> start(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(assessmentService.startAssessment(principal.id()));
    }

    @Operation(summary = "Submit self-ratings — persists topic scores to the proficiency table")
    @PostMapping("/submit")
    public ResponseEntity<Void> submit(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AssessmentSubmitRequest request) {
        assessmentService.submitRatings(principal.id(), request.ratings());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Get proficiency report — topic scores, overall level, gaps and quick wins",
        responses = {
            @ApiResponse(responseCode = "200", description = "Assessment report for the authenticated user"),
            @ApiResponse(responseCode = "403", description = "userId does not match the authenticated principal")
        }
    )
    @GetMapping("/report/{userId}")
    public ResponseEntity<AssessmentReport> report(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID userId) {
        assertOwnership(principal, userId);
        return ResponseEntity.ok(assessmentService.generateReport(userId));
    }

    private void assertOwnership(AuthenticatedUser principal, UUID userId) {
        if (!principal.id().equals(userId)) {
            log.warn("Assessment report access denied: principal={} requestedUserId={}", principal.id(), userId);
            throw new AssessmentException(
                ErrorCode.ASSESSMENT_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                "You do not have access to assessment report for user " + userId
            );
        }
    }
}
