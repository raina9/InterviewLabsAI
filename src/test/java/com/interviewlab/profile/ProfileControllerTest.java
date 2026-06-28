package com.interviewlab.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.interviewlab.ai.AiProvider;
import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import com.interviewlab.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean UserProfileService    userProfileService;
    @MockitoBean JwtService            jwtService;
    @MockitoBean OAuth2SuccessHandler  oauth2SuccessHandler;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private UserProfile profileFixture() {
        return new UserProfile(USER_ID, AiProvider.GEMINI);
    }

    @Test
    void getProfile_authenticated_returns200() throws Exception {
        when(userProfileService.getOrCreateProfile(USER_ID)).thenReturn(profileFixture());

        mockMvc.perform(get("/api/v1/profile").with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
            .andExpect(jsonPath("$.preferredAiProvider").value("GEMINI"));
    }

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_validRequest_returns200() throws Exception {
        UserProfile profile = profileFixture();
        profile.setExperienceYears(5);
        profile.setCurrentRole("Senior Engineer");
        when(userProfileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class))).thenReturn(profile);

        UpdateProfileRequest request = new UpdateProfileRequest(5, "Senior Engineer", null, null);

        mockMvc.perform(put("/api/v1/profile")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.experienceYears").value(5))
            .andExpect(jsonPath("$.currentRole").value("Senior Engineer"));
    }

    @Test
    void updateResume_validRequest_returns200() throws Exception {
        UserProfile profile = profileFixture();
        profile.setResumeText("10 years Java experience...");
        when(userProfileService.updateResumeText(eq(USER_ID), eq("10 years Java experience..."))).thenReturn(profile);

        UpdateResumeRequest request = new UpdateResumeRequest("10 years Java experience...");

        mockMvc.perform(put("/api/v1/profile/resume")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumeText").value("10 years Java experience..."));
    }

    @Test
    void updateCustomPrompt_validRequest_returns200() throws Exception {
        UserProfile profile = profileFixture();
        profile.setCustomPrompt("Focus on system design");
        when(userProfileService.updateCustomPrompt(eq(USER_ID), eq("Focus on system design"))).thenReturn(profile);

        UpdateCustomPromptRequest request = new UpdateCustomPromptRequest("Focus on system design");

        mockMvc.perform(put("/api/v1/profile/custom-prompt")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customPrompt").value("Focus on system design"));
    }
}
