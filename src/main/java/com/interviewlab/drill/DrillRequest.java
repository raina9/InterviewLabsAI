package com.interviewlab.drill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DrillRequest(
    @NotBlank String   topic,
    @NotNull  DrillMode mode
) {}
