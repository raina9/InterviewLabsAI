package com.interviewlab.assessment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssessmentSubmitRequest(
    @NotEmpty @Valid List<TopicRating> ratings
) {}
