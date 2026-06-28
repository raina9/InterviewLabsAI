package com.interviewlab.profile;

import com.interviewlab.ai.AiProvider;

/**
 * Partial-update DTO for profile metadata.
 * All fields are nullable — only non-null fields are applied by the service.
 * Resume and custom-prompt have dedicated endpoints.
 */
public record UpdateProfileRequest(
    Integer   experienceYears,
    String    currentRole,
    String[]  techStack,
    AiProvider preferredAiProvider
) {}
