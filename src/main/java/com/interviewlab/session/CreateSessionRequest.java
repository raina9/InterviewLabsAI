package com.interviewlab.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
    @NotNull  InterviewType interviewType,
    @NotBlank String        targetRole,
    @NotBlank String        jdText,
    @NotBlank String        difficulty,
    String                  targetCompany,
    String                  topicFocus
) {}
