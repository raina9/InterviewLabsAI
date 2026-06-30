package com.interviewlab.drill;

import jakarta.validation.constraints.NotBlank;

public record DrillAnswerRequest(@NotBlank String answer) {}
