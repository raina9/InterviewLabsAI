package com.interviewlab.code;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CodeSubmitRequest(
    @NotNull  UUID   challengeId,
    @NotBlank String code,
    @NotBlank String language
) {}
