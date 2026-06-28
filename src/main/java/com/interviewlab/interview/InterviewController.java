package com.interviewlab.interview;

import com.interviewlab.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/interview")
@Tag(name = "Interview", description = "End-to-end interview flow — start session, submit answers, retrieve feedback")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    @Operation(summary = "Start an interview session",
               description = "Initiates the session and returns the first AI-generated question. Session must be ACTIVE and not yet started.")
    public ResponseEntity<InterviewStartResponse> startInterview(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid StartInterviewRequest request) {
        InterviewStartResponse response = interviewService.startInterview(principal.id(), request.sessionId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/respond")
    @Operation(summary = "Submit a candidate answer",
               description = "Persists the answer, generates the next question, runs the mentor loop, and returns feedback. Session must be ACTIVE.")
    public ResponseEntity<InterviewTurnResponse> respond(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid CandidateResponseRequest request) {
        InterviewTurnResponse response = interviewService.respond(
            principal.id(), sessionId, request.answer(), request.voiceUsed()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}/feedback")
    @Operation(summary = "Get all mentor feedback for a session",
               description = "Returns all MentorAgent feedback entries for the session, ordered by persistence time. Ownership is enforced.")
    public ResponseEntity<List<MentorFeedbackResponse>> getFeedback(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<MentorFeedbackResponse> feedback = interviewService.getFeedback(principal.id(), sessionId);
        return ResponseEntity.ok(feedback);
    }
}
