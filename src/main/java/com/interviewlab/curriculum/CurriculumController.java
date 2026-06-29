package com.interviewlab.curriculum;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/curriculum")
@Tag(name = "Curriculum", description = "AI-generated personalised learning plan based on proficiency report")
public class CurriculumController {

    private final CurriculumService curriculumService;

    @Operation(summary = "Generate curriculum — personalised learning plan ordered by priority")
    @GetMapping("/{userId}")
    public ResponseEntity<CurriculumPlan> generateCurriculum(@PathVariable UUID userId) {
        return ResponseEntity.ok(curriculumService.generatePlan(userId));
    }
}
