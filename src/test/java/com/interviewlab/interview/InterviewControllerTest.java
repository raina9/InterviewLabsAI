package com.interviewlab.interview;

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

@WebMvcTest(InterviewController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class InterviewControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean InterviewService      interviewService;
    @MockitoBean JwtService            jwtService;
    @MockitoBean OAuth2SuccessHandler  oauth2SuccessHandler;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID MSG_ID     = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private MentorFeedbackResponse sampleFeedback() {
        return new MentorFeedbackResponse("Good", "Add examples", "Refined...", "Model...", "Note", 7);
    }

    // -------------------------------------------------------------------------
    // POST /start
    // -------------------------------------------------------------------------

    @Test
    void start_validRequest_returns200WithFirstQuestion() throws Exception {
        InterviewStartResponse response = new InterviewStartResponse(SESSION_ID, "Tell me about yourself.", 10);
        when(interviewService.startInterview(eq(USER_ID), eq(SESSION_ID))).thenReturn(response);

        StartInterviewRequest request = new StartInterviewRequest(SESSION_ID);

        mockMvc.perform(post("/api/v1/interview/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
            .andExpect(jsonPath("$.firstQuestion").value("Tell me about yourself."))
            .andExpect(jsonPath("$.totalQuestions").value(10));
    }

    @Test
    void start_unauthenticated_returns401() throws Exception {
        StartInterviewRequest request = new StartInterviewRequest(SESSION_ID);

        mockMvc.perform(post("/api/v1/interview/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void start_nullSessionId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/interview/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\": null}"))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /{sessionId}/respond
    // -------------------------------------------------------------------------

    @Test
    void respond_validAnswer_returns200WithFeedback() throws Exception {
        InterviewTurnResponse turnResponse = new InterviewTurnResponse("Next question?", false, sampleFeedback());
        when(interviewService.respond(eq(USER_ID), eq(SESSION_ID), eq("My detailed answer"), eq(false)))
            .thenReturn(turnResponse);

        CandidateResponseRequest request = new CandidateResponseRequest("My detailed answer", false);

        mockMvc.perform(post("/api/v1/interview/{sessionId}/respond", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agentResponse").value("Next question?"))
            .andExpect(jsonPath("$.sessionComplete").value(false))
            .andExpect(jsonPath("$.mentorFeedback.score").value(7))
            .andExpect(jsonPath("$.mentorFeedback.feedbackGood").value("Good"));
    }

    @Test
    void respond_blankAnswer_returns400() throws Exception {
        CandidateResponseRequest request = new CandidateResponseRequest("", false);

        mockMvc.perform(post("/api/v1/interview/{sessionId}/respond", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void respond_unauthenticated_returns401() throws Exception {
        CandidateResponseRequest request = new CandidateResponseRequest("My answer", false);

        mockMvc.perform(post("/api/v1/interview/{sessionId}/respond", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /{sessionId}/feedback
    // -------------------------------------------------------------------------

    @Test
    void getFeedback_authenticated_returns200WithList() throws Exception {
        List<MentorFeedbackResponse> feedbackList = List.of(sampleFeedback(), sampleFeedback());
        when(interviewService.getFeedback(USER_ID, SESSION_ID)).thenReturn(feedbackList);

        mockMvc.perform(get("/api/v1/interview/{sessionId}/feedback", SESSION_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].score").value(7));
    }

    @Test
    void getFeedback_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/interview/{sessionId}/feedback", SESSION_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getFeedback_emptySession_returns200WithEmptyList() throws Exception {
        when(interviewService.getFeedback(USER_ID, SESSION_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/interview/{sessionId}/feedback", SESSION_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
