package com.interviewlab.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizAnswerRequest(@NotBlank String answer) {}
