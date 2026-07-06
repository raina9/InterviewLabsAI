package com.interviewlab.config;

import com.interviewlab.InterviewLabApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Boots the real application (test profile — H2, stub Gemini/OAuth2 config) to prove
 * DevTokenFilter is never wired into production, independent of AUTH_MODE. This is
 * deliberately a full-context boot test, not a unit test: the guard lives inside
 * SecurityConfig.securityFilterChain(), which needs a real HttpSecurity/ServletContext
 * to construct — not something worth hand-assembling outside a real Spring context.
 */
class SecurityConfigProductionTest {

    @Test
    void productionModeWithDevAuth_applicationFailsToStart() {
        // Command-line args, not .properties(...) — SpringApplicationBuilder.properties()
        // registers "default properties" (lowest precedence), which application-test.yml's
        // own defaults would win over. Command-line args are the highest-precedence source.
        Throwable thrown = catchThrowable(() ->
            new SpringApplicationBuilder(InterviewLabApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .run(
                    "--app.deployment.mode=production",
                    "--app.auth.mode=dev",
                    "--app.auth.auto-detect=false"
                )
        );

        assertThat(thrown).isNotNull();
        Throwable root = thrown;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root).isInstanceOf(IllegalStateException.class);
        assertThat(root.getMessage()).contains("DevTokenFilter");
    }
}
