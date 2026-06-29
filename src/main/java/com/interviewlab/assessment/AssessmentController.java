package com.interviewlab.assessment;

import com.interviewlab.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Operation(summary = "Get proficiency report — topic scores, overall level, gaps and quick wins")
    @GetMapping("/report/{userId}")
    public ResponseEntity<AssessmentReport> report(@PathVariable UUID userId) {
        return ResponseEntity.ok(assessmentService.generateReport(userId));
    }
}
