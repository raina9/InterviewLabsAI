package com.interviewlab.curriculum;

import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import com.interviewlab.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurriculumController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CurriculumControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CurriculumService    curriculumService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private CurriculumPlan samplePlan() {
        List<CurriculumItem> items = List.of(
            new CurriculumItem("Kafka", "HIGH", "Critical gap — blocking senior-level interviews.", 14,
                List.of("Producers and consumers", "Consumer groups", "Partitions")),
            new CurriculumItem("Docker", "MEDIUM", "Expected in cloud-native roles.", 7,
                List.of("Dockerfile", "docker-compose", "Layered builds"))
        );
        return new CurriculumPlan(items, 3, "Backend engineering and distributed systems");
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/curriculum/{userId}
    // -------------------------------------------------------------------------

    @Test
    void generateCurriculum_withAssessmentData_returns200WithPlan() throws Exception {
        when(curriculumService.generatePlan(USER_ID)).thenReturn(samplePlan());

        mockMvc.perform(get("/api/v1/curriculum/{userId}", USER_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.estimatedWeeks").value(3))
            .andExpect(jsonPath("$.focus").isNotEmpty())
            .andExpect(jsonPath("$.items[0].topic").value("Kafka"))
            .andExpect(jsonPath("$.items[0].priority").value("HIGH"));
    }

    @Test
    void generateCurriculum_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/curriculum/{userId}", USER_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void generateCurriculum_noAssessmentData_returns500() throws Exception {
        when(curriculumService.generatePlan(USER_ID)).thenThrow(
            new CurriculumException(ErrorCode.CURRICULUM_GENERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate curriculum. Please retry your request.")
        );

        mockMvc.perform(get("/api/v1/curriculum/{userId}", USER_ID)
                .with(authentication(authToken())))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("CURRICULUM_GENERATION_FAILED"));
    }
}
