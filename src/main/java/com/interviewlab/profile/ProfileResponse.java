package com.interviewlab.profile;

import com.interviewlab.ai.AiProvider;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
    UUID       userId,
    Integer    experienceYears,
    String     currentRole,
    String[]   techStack,
    String     resumeText,
    String     customPrompt,
    AiProvider preferredAiProvider,
    Instant    updatedAt
) {
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
            profile.getUserId(),
            profile.getExperienceYears(),
            profile.getCurrentRole(),
            profile.getTechStack(),
            profile.getResumeText(),
            profile.getCustomPrompt(),
            profile.getPreferredAiProvider(),
            profile.getUpdatedAt()
        );
    }
}
