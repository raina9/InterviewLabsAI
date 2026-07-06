package com.interviewlab.feedback;

import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import com.interviewlab.auth.Role;
import com.interviewlab.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemFeedbackController.class)
@Import(SecurityConfig.class)
class SystemFeedbackControllerTest {

    @Autowired MockMvc mockMvc;

    // Required by SecurityConfig — provide mocks so the context loads
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    @MockitoBean SystemFeedbackService systemFeedbackService;

    private static final UUID FEEDBACK_ID = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Scenario 1: ADMIN caller — toggles applied, 204
    // -------------------------------------------------------------------------

    @Test
    void toggleApplied_adminCaller_returns204() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(
            UUID.randomUUID(), "admin@example.com", "Admin User", null, Role.ADMIN
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(admin, null, List.of());

        mockMvc.perform(patch("/api/v1/admin/system-feedback/{id}/applied", FEEDBACK_ID)
                .with(authentication(auth))
                .contentType("application/json")
                .content("{\"applied\": true}"))
            .andExpect(status().isNoContent());

        verify(systemFeedbackService).setApplied(FEEDBACK_ID, true);
    }

    // -------------------------------------------------------------------------
    // Scenario 2: CANDIDATE (non-admin) caller — 403 FORBIDDEN_ADMIN_ONLY
    // -------------------------------------------------------------------------

    @Test
    void toggleApplied_candidateCaller_returns403ForbiddenAdminOnly() throws Exception {
        AuthenticatedUser candidate = new AuthenticatedUser(
            UUID.randomUUID(), "candidate@example.com", "Candidate User", null, Role.CANDIDATE
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(candidate, null, List.of());

        mockMvc.perform(patch("/api/v1/admin/system-feedback/{id}/applied", FEEDBACK_ID)
                .with(authentication(auth))
                .contentType("application/json")
                .content("{\"applied\": true}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode", org.hamcrest.Matchers.is("FORBIDDEN_ADMIN_ONLY")));
    }
}
