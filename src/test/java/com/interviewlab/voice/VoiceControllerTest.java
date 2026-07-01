package com.interviewlab.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import com.interviewlab.config.SecurityConfig;
import com.interviewlab.interview.InterviewService;
import com.interviewlab.interview.InterviewTurnResponse;
import com.interviewlab.interview.MentorFeedbackResponse;
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

@WebMvcTest(VoiceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class VoiceControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean InterviewService     interviewService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private MentorFeedbackResponse sampleFeedback() {
        return new MentorFeedbackResponse("Good explanation", "Add concrete examples", "Refined...", "Model...", "Stay calm and structured", 8);
    }

    // -------------------------------------------------------------------------
    // Scenario 1: valid transcript → 200 with InterviewTurnResponse
    // -------------------------------------------------------------------------

    @Test
    void transcript_validRequest_returns200WithFeedback() throws Exception {
        InterviewTurnResponse turnResponse = new InterviewTurnResponse("Next question?", false, sampleFeedback(), null);
        when(interviewService.respond(eq(USER_ID), eq(SESSION_ID), eq("My detailed answer about Java"), eq(true)))
            .thenReturn(turnResponse);

        VoiceTranscriptRequest request = new VoiceTranscriptRequest(SESSION_ID, "My detailed answer about Java");

        mockMvc.perform(post("/api/v1/voice/transcript")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agentResponse").value("Next question?"))
            .andExpect(jsonPath("$.sessionComplete").value(false))
            .andExpect(jsonPath("$.mentorFeedback.score").value(8))
            .andExpect(jsonPath("$.mentorFeedback.feedbackGood").value("Good explanation"));
    }

    // -------------------------------------------------------------------------
    // Scenario 2: blank transcript → 400
    // -------------------------------------------------------------------------

    @Test
    void transcript_blankTranscript_returns400() throws Exception {
        VoiceTranscriptRequest request = new VoiceTranscriptRequest(SESSION_ID, "");

        mockMvc.perform(post("/api/v1/voice/transcript")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Scenario 3: unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    void transcript_unauthenticated_returns401() throws Exception {
        VoiceTranscriptRequest request = new VoiceTranscriptRequest(SESSION_ID, "My answer via voice");

        mockMvc.perform(post("/api/v1/voice/transcript")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
