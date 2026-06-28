package com.interviewlab.interview;

import jakarta.validation.constraints.NotBlank;

public record CandidateResponseRequest(
    @NotBlank String answer,
    boolean voiceUsed
) {}
