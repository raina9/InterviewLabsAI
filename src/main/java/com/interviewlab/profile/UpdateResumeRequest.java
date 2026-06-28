package com.interviewlab.profile;

import jakarta.validation.constraints.NotBlank;

public record UpdateResumeRequest(@NotBlank String resumeText) {}
