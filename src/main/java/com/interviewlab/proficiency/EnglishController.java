package com.interviewlab.proficiency;

import com.interviewlab.auth.AuthenticatedUser;
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
@RequestMapping("/api/v1/english")
@Tag(name = "English", description = "English proficiency coaching — AI analysis of spoken or written transcripts")
public class EnglishController {

    private final EnglishService englishService;

    @PostMapping("/analyze")
    @Operation(
        summary = "Analyse English proficiency",
        description = "Accepts a transcript and returns structured English coaching feedback including fluency score, " +
                      "grammar notes, filler word detection, vocabulary assessment, and an improved version."
    )
    public ResponseEntity<EnglishAnalysisResponse> analyze(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid EnglishAnalysisRequest request) {
        log.debug("English analysis request: userId={} contextType={}", principal.id(), request.context());
        EnglishFeedback feedback = englishService.analyze(
            request.transcript(), request.context(), principal.id()
        );
        return ResponseEntity.ok(EnglishAnalysisResponse.from(feedback));
    }
}
