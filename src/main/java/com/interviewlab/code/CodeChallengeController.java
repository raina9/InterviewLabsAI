package com.interviewlab.code;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/code")
@Tag(name = "Code Challenge", description = "AI-generated coding challenges with optional Judge0 execution")
public class CodeChallengeController {

    private final CodeChallengeService codeChallengeService;

    @Operation(summary = "Generate a new coding challenge for the specified topic and difficulty")
    @PostMapping("/challenge")
    public ResponseEntity<CodeChallenge> challenge(@Valid @RequestBody CodeChallengeRequest request) {
        return ResponseEntity.ok(codeChallengeService.generateChallenge(request));
    }

    @Operation(summary = "Submit a code solution — evaluated via Judge0 (if configured) or AI code review")
    @PostMapping("/submit")
    public ResponseEntity<CodeSubmitResponse> submit(@Valid @RequestBody CodeSubmitRequest request) {
        return ResponseEntity.ok(codeChallengeService.submitSolution(request));
    }

    @Operation(summary = "Get a hint for a code challenge without revealing the full solution")
    @GetMapping("/challenge/{id}/hint")
    public ResponseEntity<String> hint(@PathVariable UUID id) {
        return ResponseEntity.ok(codeChallengeService.getHint(id));
    }
}
