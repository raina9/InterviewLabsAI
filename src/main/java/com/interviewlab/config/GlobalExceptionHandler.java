package com.interviewlab.config;

import com.interviewlab.assessment.AssessmentException;
import com.interviewlab.auth.AuthException;
import com.interviewlab.ai.AIProviderException;
import com.interviewlab.code.CodeChallengeException;
import com.interviewlab.curriculum.CurriculumException;
import com.interviewlab.drill.DrillException;
import com.interviewlab.quiz.QuizException;
import com.interviewlab.interview.InterviewException;
import com.interviewlab.profile.ProfileException;
import com.interviewlab.ratelimit.RateLimitException;
import com.interviewlab.session.SessionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 * Error handling standard applied here — no internals, no stack traces in response body.
 * All exceptions are logged; only safe messages reach the API consumer.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthException ex) {
        log.warn("Auth error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<ApiError> handleAIProviderException(AIProviderException ex) {
        log.warn("AI provider error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(SessionException.class)
    public ResponseEntity<ApiError> handleSessionException(SessionException ex) {
        log.warn("Session error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(InterviewException.class)
    public ResponseEntity<ApiError> handleInterviewException(InterviewException ex) {
        log.warn("Interview error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(ProfileException.class)
    public ResponseEntity<ApiError> handleProfileException(ProfileException ex) {
        log.warn("Profile error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(AssessmentException.class)
    public ResponseEntity<ApiError> handleAssessmentException(AssessmentException ex) {
        log.warn("Assessment error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(CurriculumException.class)
    public ResponseEntity<ApiError> handleCurriculumException(CurriculumException ex) {
        log.warn("Curriculum error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(QuizException.class)
    public ResponseEntity<ApiError> handleQuizException(QuizException ex) {
        log.warn("Quiz error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(CodeChallengeException.class)
    public ResponseEntity<ApiError> handleCodeChallengeException(CodeChallengeException ex) {
        log.warn("Code challenge error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(DrillException.class)
    public ResponseEntity<ApiError> handleDrillException(DrillException ex) {
        log.warn("Drill error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimitException(RateLimitException ex) {
        log.warn("Rate limit error: code={} message={}", ex.errorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.status())
                .body(new ApiError(ex.errorCode().name(), ex.getMessage(), ex.status().value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.debug("Validation failed: {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_FAILED", details, HttpStatus.BAD_REQUEST.value()));
    }

    // Catch-all: log the real cause internally, return a safe generic message to the consumer
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred. Please try again or contact support.",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }
}
