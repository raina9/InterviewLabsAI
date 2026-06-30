package com.interviewlab.drill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import com.interviewlab.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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

@WebMvcTest(DrillController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class DrillControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean DrillService         drillService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private DrillSession sampleSession() {
        return new DrillSession(SESSION_ID, "Java", DrillMode.RAPID,
            "What is the difference between HashMap and Hashtable?", 0, false);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/drill/start
    // -------------------------------------------------------------------------

    @Test
    void start_validRapidRequest_returns200WithFirstQuestion() throws Exception {
        when(drillService.startDrill(any(DrillRequest.class))).thenReturn(sampleSession());

        mockMvc.perform(post("/api/v1/drill/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Java\",\"mode\":\"RAPID\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
            .andExpect(jsonPath("$.topic").value("Java"))
            .andExpect(jsonPath("$.mode").value("RAPID"))
            .andExpect(jsonPath("$.currentQuestion").isNotEmpty())
            .andExpect(jsonPath("$.complete").value(false));
    }

    @Test
    void start_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/drill/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Java\",\"mode\":\"RAPID\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void start_blankTopic_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/drill/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"\",\"mode\":\"RAPID\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void start_nullMode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/drill/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Java\"}"))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/drill/{sessionId}/next
    // -------------------------------------------------------------------------

    @Test
    void next_validAnswer_returns200WithNextQuestion() throws Exception {
        DrillQuestionResponse response = new DrillQuestionResponse(
            "Explain the Java memory model.", 2, false,
            "Good — HashMap is not synchronized while Hashtable is.", 7
        );
        when(drillService.nextQuestion(eq(SESSION_ID), any(DrillAnswerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/drill/{sessionId}/next", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"HashMap is not synchronized, Hashtable is.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.question").value("Explain the Java memory model."))
            .andExpect(jsonPath("$.previousScore").value(7))
            .andExpect(jsonPath("$.sessionComplete").value(false));
    }

    @Test
    void next_blankAnswer_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/drill/{sessionId}/next", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void next_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/drill/{sessionId}/next", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"any\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void next_unknownSession_returns404() throws Exception {
        when(drillService.nextQuestion(eq(SESSION_ID), any())).thenThrow(
            new DrillException(ErrorCode.DRILL_SESSION_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Drill session " + SESSION_ID + " not found.")
        );

        mockMvc.perform(post("/api/v1/drill/{sessionId}/next", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"some answer\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("DRILL_SESSION_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/drill/{sessionId}/summary
    // -------------------------------------------------------------------------

    @Test
    void summary_completedSession_returns200() throws Exception {
        DrillSummary summary = new DrillSummary(
            "Java", DrillMode.RAPID, 10, 6.5,
            List.of("What does volatile do?"),
            List.of("Explain HashMap vs Hashtable.", "What is autoboxing?")
        );
        when(drillService.getSummary(SESSION_ID)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/drill/{sessionId}/summary", SESSION_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topic").value("Java"))
            .andExpect(jsonPath("$.questionsAnswered").value(10))
            .andExpect(jsonPath("$.weakSpots").isArray())
            .andExpect(jsonPath("$.strongPoints").isArray());
    }

    @Test
    void summary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/drill/{sessionId}/summary", SESSION_ID))
            .andExpect(status().isUnauthorized());
    }
}
