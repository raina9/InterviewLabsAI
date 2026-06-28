package com.interviewlab.profile;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomPromptRequest(@NotBlank String customPrompt) {}
