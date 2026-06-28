package com.interviewlab.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.config.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Dev-mode only. Authenticates requests via X-Dev-Token header against app.auth.dev-token.
 * Active when app.auth.mode=dev; skipped entirely when mode=oauth (SecurityConfig does not register it).
 * Token present + match: sets AuthenticatedUser in SecurityContext.
 * Token present + mismatch: 401 — does not continue the filter chain.
 * Token absent: continues unauthenticated — Spring Security access rules handle protected endpoints.
 */
@Slf4j
@RequiredArgsConstructor
public class DevTokenFilter extends OncePerRequestFilter {

    static final String DEV_TOKEN_HEADER = "X-Dev-Token";

    private final AuthProperties authProperties;
    private final ObjectMapper   objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return "oauth".equalsIgnoreCase(authProperties.mode());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain
    ) throws ServletException, IOException {

        String token = request.getHeader(DEV_TOKEN_HEADER);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authProperties.devToken().equals(token)) {
            log.warn("Dev token rejected: path={}", request.getRequestURI());
            writeUnauthorized(response, "Invalid dev token. Verify the X-Dev-Token header value.");
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(
            authProperties.devUserId(),
            authProperties.devUserEmail(),
            authProperties.devUserName(),
            null
        );

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        log.debug("Dev token authenticated: user={} path={}", principal.email(), request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
            response.getWriter(),
            new ApiError("DEV_TOKEN_INVALID", message, HttpStatus.UNAUTHORIZED.value())
        );
    }
}
