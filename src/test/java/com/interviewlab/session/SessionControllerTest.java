package com.interviewlab.session;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SessionControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean SessionService       sessionService;
    @MockitoBean MessageService       messageService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private Session activeSession() {
        return new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD text", "MEDIUM", SessionStatus.ACTIVE);
    }

    @Test
    void createSession_validRequest_returns201() throws Exception {
        when(sessionService.createSession(eq(USER_ID), any(CreateSessionRequest.class))).thenReturn(activeSession());

        CreateSessionRequest request = new CreateSessionRequest(
            InterviewType.TECHNICAL, "Senior Engineer", "JD text", "MEDIUM", null, null
        );

        mockMvc.perform(post("/api/v1/sessions")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.interviewType").value("TECHNICAL"))
            .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void createSession_unauthenticated_returns401() throws Exception {
        CreateSessionRequest request = new CreateSessionRequest(
            InterviewType.TECHNICAL, "Senior Engineer", "JD text", "MEDIUM", null, null
        );

        mockMvc.perform(post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listSessions_authenticated_returns200WithList() throws Exception {
        when(sessionService.getUserSessions(USER_ID)).thenReturn(List.of(activeSession(), activeSession()));

        mockMvc.perform(get("/api/v1/sessions").with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSession_exists_returns200() throws Exception {
        when(sessionService.getSession(SESSION_ID, USER_ID)).thenReturn(activeSession());

        mockMvc.perform(get("/api/v1/sessions/{id}", SESSION_ID).with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void completeSession_returns200WithCompletedStatus() throws Exception {
        Session completed = activeSession();
        completed.setStatus(SessionStatus.COMPLETED);
        when(sessionService.completeSession(SESSION_ID, USER_ID)).thenReturn(completed);

        mockMvc.perform(post("/api/v1/sessions/{id}/complete", SESSION_ID).with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void abandonSession_returns200WithAbandonedStatus() throws Exception {
        Session abandoned = activeSession();
        abandoned.setStatus(SessionStatus.ABANDONED);
        when(sessionService.abandonSession(SESSION_ID, USER_ID)).thenReturn(abandoned);

        mockMvc.perform(post("/api/v1/sessions/{id}/abandon", SESSION_ID).with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ABANDONED"));
    }

    @Test
    void getMessages_authenticated_returns200WithMessages() throws Exception {
        when(sessionService.getSession(SESSION_ID, USER_ID)).thenReturn(activeSession());
        Message msg = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Tell me about yourself.", 1, false);
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/v1/sessions/{id}/messages", SESSION_ID).with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].role").value("INTERVIEWER"))
            .andExpect(jsonPath("$[0].sequence").value(1));
    }
}
