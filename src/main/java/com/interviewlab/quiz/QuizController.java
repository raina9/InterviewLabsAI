package com.interviewlab.quiz;

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
@RequestMapping("/api/v1/quiz")
@Tag(name = "Quiz", description = "AI-generated multiple-choice quiz with instant scoring")
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Start a new quiz session — generates questions for the specified topic and difficulty")
    @PostMapping("/start")
    public ResponseEntity<QuizSession> start(@Valid @RequestBody StartQuizRequest request) {
        return ResponseEntity.ok(quizService.startQuiz(request));
    }

    @Operation(summary = "Submit an answer for the current question — returns correctness, explanation, and next question")
    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<QuizAnswerResponse> answer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody QuizAnswerRequest request) {
        return ResponseEntity.ok(quizService.submitAnswer(sessionId, request.answer()));
    }

    @Operation(summary = "Get the final result of a completed quiz session")
    @GetMapping("/{sessionId}/result")
    public ResponseEntity<QuizResult> result(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(quizService.getResult(sessionId));
    }
}
