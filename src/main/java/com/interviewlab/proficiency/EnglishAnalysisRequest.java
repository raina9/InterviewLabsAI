package com.interviewlab.proficiency;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record EnglishAnalysisRequest(
    @NotBlank String transcript,
    UUID sessionId,  // nullable — links analysis to a session for future cross-reference
    String context   // optional — "interview", "casual", "presentation"
) {}
