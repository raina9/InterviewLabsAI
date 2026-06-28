package com.interviewlab.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the JWT from the httpOnly cookie on every request.
 * Populates SecurityContext when token is valid; clears bad cookie and continues
 * as unauthenticated when token is missing or invalid.
 * Never throws — protected endpoints rely on Spring Security access rules, not this filter.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String JWT_COOKIE_NAME = "jwt";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.verifyToken(token);
            AuthenticatedUser principal = jwtService.extractPrincipal(claims);

            // No credentials needed — we trust the verified JWT claims
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT authenticated: user={} path={}", principal.email(), request.getRequestURI());

        } catch (AuthException e) {
            // Clear the stale or invalid cookie so the browser does not keep sending it
            clearJwtCookie(response);
            log.debug("JWT rejected on {}: {}", request.getRequestURI(), e.errorCode());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> JWT_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie empty = new Cookie(JWT_COOKIE_NAME, "");
        empty.setHttpOnly(true);
        empty.setPath("/");
        empty.setMaxAge(0);
        response.addCookie(empty);
    }
}
