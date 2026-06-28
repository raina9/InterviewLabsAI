package com.interviewlab.interview;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartInterviewRequest(
    @NotNull UUID sessionId
) {}
