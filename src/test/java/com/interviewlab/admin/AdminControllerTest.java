package com.interviewlab.admin;

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
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    // Required by SecurityConfig — provide mocks so the context loads
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    @MockitoBean AdminStatsService adminStatsService;

    // -------------------------------------------------------------------------
    // Scenario 1: ADMIN caller — 200 with stats body
    // -------------------------------------------------------------------------

    @Test
    void getStats_adminCaller_returns200WithStats() throws Exception {
        AuthenticatedUser admin = new AuthenticatedUser(
            UUID.randomUUID(), "admin@example.com", "Admin User", null, Role.ADMIN
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(admin, null, List.of());

        AdminStatsResponse stats = new AdminStatsResponse(
            5, 42, 3, 7.5,
            Map.of("interview", 42L, "quiz", 0L, "drill", 0L, "code", 0L),
            12
        );
        when(adminStatsService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats")
                .with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionsToday").value(5))
            .andExpect(jsonPath("$.sessionsTotal").value(42))
            .andExpect(jsonPath("$.activeUsersToday").value(3))
            .andExpect(jsonPath("$.avgSessionScore").value(7.5))
            .andExpect(jsonPath("$.featureUsage.interview").value(42))
            .andExpect(jsonPath("$.aiCallsToday").value(12));

        verify(adminStatsService).getStats();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: CANDIDATE (non-admin) caller — 403 FORBIDDEN_ADMIN_ONLY
    // -------------------------------------------------------------------------

    @Test
    void getStats_candidateCaller_returns403ForbiddenAdminOnly() throws Exception {
        AuthenticatedUser candidate = new AuthenticatedUser(
            UUID.randomUUID(), "candidate@example.com", "Candidate User", null, Role.CANDIDATE
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(candidate, null, List.of());

        mockMvc.perform(get("/api/v1/admin/stats")
                .with(authentication(auth)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode", org.hamcrest.Matchers.is("FORBIDDEN_ADMIN_ONLY")));
    }
}
