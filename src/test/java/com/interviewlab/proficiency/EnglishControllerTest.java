package com.interviewlab.proficiency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnglishController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class EnglishControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean EnglishService       englishService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private EnglishFeedback sampleFeedback() {
        return new EnglishFeedback(
            "Clear delivery", "No errors detected", "um, uh",
            "Good vocabulary", "Sounds confident", "Improved version here", 8
        );
    }

    // -------------------------------------------------------------------------
    // Scenario 1: valid transcript → 200 with EnglishAnalysisResponse
    // -------------------------------------------------------------------------

    @Test
    void analyze_validRequest_returns200WithFeedback() throws Exception {
        when(englishService.analyze(eq("I am a backend developer"), eq("interview"), eq(USER_ID)))
            .thenReturn(sampleFeedback());

        EnglishAnalysisRequest request = new EnglishAnalysisRequest(
            "I am a backend developer", null, "interview"
        );

        mockMvc.perform(post("/api/v1/english/analyze")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.feedback.fluencyScore").value(8))
            .andExpect(jsonPath("$.feedback.fluencyNote").value("Clear delivery"))
            .andExpect(jsonPath("$.feedback.fillerWordsDetected").value("um, uh"));
    }

    // -------------------------------------------------------------------------
    // Scenario 2: blank transcript → 400
    // -------------------------------------------------------------------------

    @Test
    void analyze_blankTranscript_returns400() throws Exception {
        EnglishAnalysisRequest request = new EnglishAnalysisRequest("", null, "interview");

        mockMvc.perform(post("/api/v1/english/analyze")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Scenario 3: unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    void analyze_unauthenticated_returns401() throws Exception {
        EnglishAnalysisRequest request = new EnglishAnalysisRequest(
            "I am a developer", null, "interview"
        );

        mockMvc.perform(post("/api/v1/english/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
