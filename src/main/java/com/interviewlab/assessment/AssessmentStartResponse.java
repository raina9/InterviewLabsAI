package com.interviewlab.assessment;

import java.util.List;

public record AssessmentStartResponse(
    List<String> topics,
    String       instructions
) {}
