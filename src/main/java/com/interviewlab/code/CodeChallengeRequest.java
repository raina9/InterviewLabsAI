package com.interviewlab.code;

import jakarta.validation.constraints.NotBlank;

public record CodeChallengeRequest(
    @NotBlank String topic,
    @NotBlank String difficulty
) {}
