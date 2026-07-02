package com.interviewlab.auth;

import com.interviewlab.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    // Required by SecurityConfig — provide mocks so the context loads
    @MockitoBean JwtService       jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID TEST_ID = UUID.randomUUID();

    // Authentication is supplied via SecurityMockMvcRequestPostProcessors, not manual
    // SecurityContextHolder mutation: Spring Security 6+'s SecurityContextHolderFilter
    // reloads/clears the context around every request, so a context set directly on the
    // ThreadLocal before mockMvc.perform() never survives into the filter chain
    // (see docs/lld/auth-flow.md and session-notes v32).
    @Test
    void me_withAuthenticatedPrincipal_returnsUserResponse() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(
            TEST_ID, "user@example.com", "Test User", "https://pic.url"
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, List.of());

        mockMvc.perform(get("/api/v1/auth/me").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("user@example.com")))
            .andExpect(jsonPath("$.name",  is("Test User")));
    }

    @Test
    void me_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").with(anonymous()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookieAndReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie(JwtAuthFilter.JWT_COOKIE_NAME, "some-token")))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge(JwtAuthFilter.JWT_COOKIE_NAME, 0));
    }
}
