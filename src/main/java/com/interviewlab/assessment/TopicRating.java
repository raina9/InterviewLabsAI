package com.interviewlab.assessment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TopicRating(
    @NotBlank String topic,
    @Min(1) @Max(10) int rating
) {}
