package com.interviewlab.profile;

import com.interviewlab.ai.AiProperties;
import com.interviewlab.auth.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AiProperties          aiProperties;

    @Transactional
    public UserProfile getOrCreateProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserProfile created = userProfileRepository.save(
                    new UserProfile(userId, aiProperties.defaultProvider())
                );
                log.info("Profile created: userId={} preferredAiProvider={}", userId, created.getPreferredAiProvider());
                return created;
            });
    }

    @Transactional
    public UserProfile updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = findProfileOrThrow(userId);
        if (request.experienceYears()     != null) profile.setExperienceYears(request.experienceYears());
        if (request.currentRole()         != null) profile.setCurrentRole(request.currentRole());
        if (request.techStack()           != null) profile.setTechStack(request.techStack());
        if (request.preferredAiProvider() != null) profile.setPreferredAiProvider(request.preferredAiProvider());
        log.debug("Profile metadata updated: userId={}", userId);
        return userProfileRepository.save(profile);
    }

    @Transactional
    public UserProfile updateResumeText(UUID userId, String resumeText) {
        UserProfile profile = findProfileOrThrow(userId);
        profile.setResumeText(resumeText);
        log.debug("Resume updated: userId={}", userId);
        return userProfileRepository.save(profile);
    }

    @Transactional
    public UserProfile updateResumeUrl(UUID userId, String resumeUrl) {
        UserProfile profile = findProfileOrThrow(userId);
        profile.setResumeUrl(resumeUrl);
        try {
            UserProfile saved = userProfileRepository.save(profile);
            log.debug("Resume URL updated: userId={}", userId);
            return saved;
        } catch (DataAccessException e) {
            throw new ProfileException(
                ErrorCode.RESUME_UPLOAD_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to save resume URL for user " + userId
            );
        }
    }

    @Transactional
    public UserProfile updateCustomPrompt(UUID userId, String customPrompt) {
        UserProfile profile = findProfileOrThrow(userId);
        profile.setCustomPrompt(customPrompt);
        log.debug("Custom prompt updated: userId={}", userId);
        return userProfileRepository.save(profile);
    }

    private UserProfile findProfileOrThrow(UUID userId) {
        return userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new ProfileException(
                ErrorCode.PROFILE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Profile not found for user " + userId
            ));
    }
}
