package com.interviewlab.profile;

import com.interviewlab.ai.AiProperties;
import com.interviewlab.ai.AiProvider;
import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock UserProfileRepository userProfileRepository;
    @Mock AiProperties          aiProperties;
    @InjectMocks UserProfileService userProfileService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void getOrCreateProfile_profileAbsent_createsWithDefaultProvider() {
        when(aiProperties.defaultProvider()).thenReturn(AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        UserProfile created = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(created);

        UserProfile result = userProfileService.getOrCreateProfile(USER_ID);

        assertThat(result.getPreferredAiProvider()).isEqualTo(AiProvider.GEMINI);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void getOrCreateProfile_profilePresent_returnsExistingWithoutCreate() {
        UserProfile existing = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

        UserProfile result = userProfileService.getOrCreateProfile(USER_ID);

        assertThat(result).isSameAs(existing);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateProfile_profileExists_appliesNonNullFields() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        UpdateProfileRequest request = new UpdateProfileRequest(5, "Senior Engineer", new String[]{"Java", "Spring"}, null);
        UserProfile result = userProfileService.updateProfile(USER_ID, request);

        assertThat(result.getExperienceYears()).isEqualTo(5);
        assertThat(result.getCurrentRole()).isEqualTo("Senior Engineer");
        assertThat(result.getTechStack()).containsExactly("Java", "Spring");
        assertThat(result.getPreferredAiProvider()).isEqualTo(AiProvider.GEMINI); // unchanged — null in request
    }

    @Test
    void updateProfile_profileNotFound_throwsProfileException() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest(3, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateProfile(USER_ID, request))
            .isInstanceOf(ProfileException.class)
            .satisfies(ex -> assertThat(((ProfileException) ex).errorCode())
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND));
    }

    @Test
    void updateResumeText_profileExists_updatesResume() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        UserProfile result = userProfileService.updateResumeText(USER_ID, "My resume text");

        assertThat(result.getResumeText()).isEqualTo("My resume text");
    }

    @Test
    void updateCustomPrompt_profileExists_updatesPrompt() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        UserProfile result = userProfileService.updateCustomPrompt(USER_ID, "Focus on system design");

        assertThat(result.getCustomPrompt()).isEqualTo("Focus on system design");
    }

    @Test
    void updateResumeUrl_profileExists_updatesResumeUrl() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);

        UserProfile result = userProfileService.updateResumeUrl(USER_ID, "/files/" + USER_ID + "/resume.pdf");

        assertThat(result.getResumeUrl()).isEqualTo("/files/" + USER_ID + "/resume.pdf");
    }

    @Test
    void updateResumeUrl_profileNotFound_throwsProfileException() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateResumeUrl(USER_ID, "/files/x.pdf"))
            .isInstanceOf(ProfileException.class)
            .satisfies(ex -> assertThat(((ProfileException) ex).errorCode())
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND));
    }

    @Test
    void updateResumeUrl_repositorySaveFails_throwsResumeUploadFailed() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("db down"));

        assertThatThrownBy(() -> userProfileService.updateResumeUrl(USER_ID, "/files/x.pdf"))
            .isInstanceOf(ProfileException.class)
            .satisfies(ex -> assertThat(((ProfileException) ex).errorCode())
                .isEqualTo(ErrorCode.RESUME_UPLOAD_FAILED));
    }
}
