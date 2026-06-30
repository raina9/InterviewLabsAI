package com.interviewlab.drill;

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
@RequestMapping("/api/v1/drill")
@Tag(name = "Topic Drill", description = "RAPID (quick-fire Q&A) and DEEP (Socratic drilling) topic practice")
public class DrillController {

    private final DrillService drillService;

    @Operation(summary = "Start a new drill session — RAPID (10 quick Q&A) or DEEP (Socratic, 8 turns)")
    @PostMapping("/start")
    public ResponseEntity<DrillSession> start(@Valid @RequestBody DrillRequest request) {
        return ResponseEntity.ok(drillService.startDrill(request));
    }

    @Operation(summary = "Submit answer to the current question and receive the next question")
    @PostMapping("/{sessionId}/next")
    public ResponseEntity<DrillQuestionResponse> next(
            @PathVariable UUID sessionId,
            @Valid @RequestBody DrillAnswerRequest request) {
        return ResponseEntity.ok(drillService.nextQuestion(sessionId, request));
    }

    @Operation(summary = "Get the final summary of a drill session — weak spots and strong areas")
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<DrillSummary> summary(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(drillService.getSummary(sessionId));
    }
}
