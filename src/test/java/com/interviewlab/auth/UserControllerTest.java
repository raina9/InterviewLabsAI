package com.interviewlab.auth;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    // Required by SecurityConfig — provide mocks so the context loads
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler  oauth2SuccessHandler;

    @MockitoBean UserAccountService userAccountService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void deleteAccount_authenticated_returns204AndDelegatesToService() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(
            USER_ID, "user@example.com", "Test User", null
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, List.of());

        mockMvc.perform(delete("/api/v1/me").with(authentication(auth)))
            .andExpect(status().isNoContent());

        verify(userAccountService).deleteAccount(USER_ID);
    }

    @Test
    void deleteAccount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/me").with(anonymous()))
            .andExpect(status().isUnauthorized());
    }
}
