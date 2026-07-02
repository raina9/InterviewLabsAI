package com.interviewlab.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.auth.AuthProperties;
import com.interviewlab.auth.DevTokenFilter;
import com.interviewlab.auth.JwtAuthFilter;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String NOT_CONFIGURED = "not-configured";

    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtService           jwtService;
    private final AuthProperties       authProperties;
    private final ObjectMapper         objectMapper;

    @Value("${GOOGLE_CLIENT_ID:not-configured}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:not-configured}")
    private String googleClientSecret;

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtService);
    }

    @Bean
    public DevTokenFilter devTokenFilter() {
        return new DevTokenFilter(authProperties, objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless REST API — no HttpSession, no JSESSIONID cookie
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF disabled: V1 uses httpOnly cookie + CORS locked to known origins (SameSite=Lax).
            // V2 public/embedded: add CSRF token header validation or switch to SameSite=Strict.
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Auth endpoints (OAuth2 callback, /me, /logout) — Spring Security permits;
                // /me enforces auth programmatically (null principal check in controller)
                .requestMatchers("/api/v1/auth/**").permitAll()

                // API documentation — public in dev; restrict in prod via env config
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**", "/api-docs/**"
                ).permitAll()

                // Health endpoint — required unauthenticated for Railway health checks
                .requestMatchers("/actuator/health").permitAll()

                // Embedded frontend static assets — CDN React shell and JSX components
                .requestMatchers("/", "/index.html", "/static/**").permitAll()

                // All other API endpoints require authentication
                .requestMatchers("/api/v1/**").authenticated()

                // Reject all non-matched paths — fail closed, not open
                .anyRequest().denyAll()
            )

            // Return HTTP 401 (not a redirect to /login) for unauthenticated API requests
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        boolean useOAuth = "oauth".equalsIgnoreCase(authProperties.mode())
            || (authProperties.autoDetect() && hasGoogleCredentials());

        if (useOAuth) {
            String reason = "oauth".equalsIgnoreCase(authProperties.mode())
                ? "explicit AUTH_MODE=oauth"
                : "auto-detected Google credentials";
            log.info("Auth mode: oauth ({})", reason);
            // OAuth mode: Google OAuth2 login active, JWT cookie validates subsequent requests
            http
                .oauth2Login(oauth2 -> oauth2
                    .successHandler(oauth2SuccessHandler)
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        } else {
            log.info("Auth mode: dev (no Google credentials — set GOOGLE_CLIENT_ID + GOOGLE_CLIENT_SECRET to enable OAuth)");
            // Dev mode: X-Dev-Token header authenticates all requests — no OAuth2 login endpoint
            http.addFilterBefore(devTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    private boolean hasGoogleCredentials() {
        return googleClientId != null
            && !NOT_CONFIGURED.equals(googleClientId)
            && !googleClientId.isBlank()
            && googleClientSecret != null
            && !NOT_CONFIGURED.equals(googleClientSecret)
            && !googleClientSecret.isBlank();
    }
}
