package com.interviewlab.session;

import com.interviewlab.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Interview session lifecycle — create, track, and close sessions")
public class SessionController {

    private final SessionService sessionService;
    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Create a new session",
               description = "Creates a new interview session in ACTIVE status.")
    public ResponseEntity<SessionResponse> createSession(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid CreateSessionRequest request) {
        Session session = sessionService.createSession(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(session));
    }

    @GetMapping
    @Operation(summary = "List user sessions",
               description = "Returns all sessions for the authenticated user, ordered by creation time.")
    public ResponseEntity<List<SessionSummaryResponse>> listSessions(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<SessionSummaryResponse> summaries = sessionService.getUserSessions(principal.id())
            .stream()
            .map(SessionSummaryResponse::from)
            .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID",
               description = "Returns full session details. Returns 403 if the session belongs to another user.")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.getSession(id, principal.id())));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a session",
               description = "Transitions an ACTIVE session to COMPLETED. Returns 409 if the session is not ACTIVE.")
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.completeSession(id, principal.id())));
    }

    @PostMapping("/{id}/abandon")
    @Operation(summary = "Abandon a session",
               description = "Transitions an ACTIVE session to ABANDONED. Returns 409 if the session is not ACTIVE.")
    public ResponseEntity<SessionResponse> abandonSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(SessionResponse.from(sessionService.abandonSession(id, principal.id())));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get session messages",
               description = "Returns the ordered message transcript for a session. Ownership is enforced.")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        sessionService.getSession(id, principal.id()); // ownership check
        List<MessageResponse> responses = messageService.getSessionMessages(id)
            .stream()
            .map(MessageResponse::from)
            .toList();
        return ResponseEntity.ok(responses);
    }
}
