package com.interviewlab.curriculum;

import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/curriculum")
@Tag(name = "Curriculum", description = "AI-generated personalised learning plan based on proficiency report")
public class CurriculumController {

    private final CurriculumService curriculumService;

    @Operation(
        summary = "Generate curriculum — personalised learning plan ordered by priority",
        responses = {
            @ApiResponse(responseCode = "200", description = "Curriculum plan for the authenticated user"),
            @ApiResponse(responseCode = "403", description = "userId does not match the authenticated principal")
        }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<CurriculumPlan> generateCurriculum(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID userId) {
        assertOwnership(principal, userId);
        return ResponseEntity.ok(curriculumService.generatePlan(userId));
    }

    private void assertOwnership(AuthenticatedUser principal, UUID userId) {
        if (!principal.id().equals(userId)) {
            log.warn("Curriculum access denied: principal={} requestedUserId={}", principal.id(), userId);
            throw new CurriculumException(
                ErrorCode.CURRICULUM_ACCESS_DENIED,
                HttpStatus.FORBIDDEN,
                "You do not have access to curriculum plan for user " + userId
            );
        }
    }
}
