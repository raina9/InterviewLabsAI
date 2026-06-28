package com.interviewlab.voice;

import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.interview.InterviewService;
import com.interviewlab.interview.InterviewTurnResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/voice")
@Tag(name = "Voice", description = "Voice transcript endpoint — delegates to the interview respond flow with voiceUsed=true")
public class VoiceController {

    private final InterviewService interviewService;

    @PostMapping("/transcript")
    @Operation(
        summary = "Submit a voice transcript",
        description = "Accepts a speech-to-text transcript and delegates to the same respond flow as text input. " +
                      "voiceUsed is always true for this endpoint — the voice path implies voice was used."
    )
    public ResponseEntity<InterviewTurnResponse> submitTranscript(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid VoiceTranscriptRequest request) {
        log.debug("Voice transcript received: sessionId={} userId={}", request.sessionId(), principal.id());
        InterviewTurnResponse response = interviewService.respond(
            principal.id(), request.sessionId(), request.transcript(), true
        );
        return ResponseEntity.ok(response);
    }
}
