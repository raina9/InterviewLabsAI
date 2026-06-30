package com.interviewlab.assessment;

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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AssessmentControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean AssessmentService    assessmentService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private AssessmentStartResponse sampleStart() {
        return new AssessmentStartResponse(
            List.of("Java", "Spring Boot", "System Design", "Kafka", "Docker"),
            "Rate each topic from 1 (no experience) to 10 (expert)"
        );
    }

    private AssessmentReport sampleReport() {
        List<TopicScore> scores = List.of(
            new TopicScore("Java", 7, "Senior", "Good foundation."),
            new TopicScore("Kafka", 3, "Beginner", "Start with fundamentals.")
        );
        return new AssessmentReport(scores, "Intermediate", List.of("Kafka"), List.of("Spring Boot"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/assessment/start
    // -------------------------------------------------------------------------

    @Test
    void start_authenticated_returns200WithTopics() throws Exception {
        when(assessmentService.startAssessment(USER_ID)).thenReturn(sampleStart());

        mockMvc.perform(post("/api/v1/assessment/start")
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topics").isArray())
            .andExpect(jsonPath("$.topics.length()").value(5))
            .andExpect(jsonPath("$.instructions").isNotEmpty());
    }

    @Test
    void start_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/assessment/start"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void start_profileNotFound_returns404() throws Exception {
        when(assessmentService.startAssessment(USER_ID)).thenThrow(
            new AssessmentException(ErrorCode.ASSESSMENT_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND,
                "User profile not found for user " + USER_ID)
        );

        mockMvc.perform(post("/api/v1/assessment/start")
                .with(authentication(authToken())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("ASSESSMENT_PROFILE_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/assessment/submit
    // -------------------------------------------------------------------------

    @Test
    void submit_validRatings_returns200() throws Exception {
        doNothing().when(assessmentService).submitRatings(eq(USER_ID), any());

        AssessmentSubmitRequest request = new AssessmentSubmitRequest(List.of(
            new TopicRating("Java", 7),
            new TopicRating("Kafka", 3)
        ));

        mockMvc.perform(post("/api/v1/assessment/submit")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void submit_emptyRatings_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/assessment/submit")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ratings\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void submit_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/assessment/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ratings\":[{\"topic\":\"Java\",\"rating\":7}]}"))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/assessment/report/{userId}
    // -------------------------------------------------------------------------

    @Test
    void report_withAssessmentData_returns200WithReport() throws Exception {
        when(assessmentService.generateReport(USER_ID)).thenReturn(sampleReport());

        mockMvc.perform(get("/api/v1/assessment/report/{userId}", USER_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallLevel").value("Intermediate"))
            .andExpect(jsonPath("$.topics").isArray())
            .andExpect(jsonPath("$.criticalGaps").isArray())
            .andExpect(jsonPath("$.quickWins").isArray());
    }

    @Test
    void report_noAssessmentData_returns404() throws Exception {
        when(assessmentService.generateReport(USER_ID)).thenThrow(
            new AssessmentException(ErrorCode.ASSESSMENT_NOT_FOUND, HttpStatus.NOT_FOUND,
                "No assessment data found for user " + USER_ID)
        );

        mockMvc.perform(get("/api/v1/assessment/report/{userId}", USER_ID)
                .with(authentication(authToken())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("ASSESSMENT_NOT_FOUND"));
    }

    @Test
    void report_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/assessment/report/{userId}", USER_ID))
            .andExpect(status().isUnauthorized());
    }
}
