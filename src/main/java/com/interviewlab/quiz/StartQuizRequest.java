package com.interviewlab.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StartQuizRequest(
    @NotBlank String topic,
    @NotBlank String difficulty,
    @Min(3) @Max(20) int questionCount
) {}
