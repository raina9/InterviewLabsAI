package com.interviewlab.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Fires after Google OAuth2 login succeeds.
 * Extracts OIDC attributes, upserts user in DB, issues JWT as httpOnly cookie,
 * then redirects to the frontend. The frontend reads user info from /api/v1/auth/me.
 *
 * SameSite=Lax: protects against cross-site request forgery on top-level navigations
 * while allowing the cookie to be sent on the OAuth2 redirect (same-origin redirect).
 * V2 embedded mode: revisit SameSite=None + Secure for cross-origin iframe use.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String ATTR_SUB     = "sub";
    private static final String ATTR_EMAIL   = "email";
    private static final String ATTR_NAME    = "name";
    private static final String ATTR_PICTURE = "picture";

    private final JwtService  jwtService;
    private final UserService userService;
    private final JwtProperties jwtProperties;

    @Value("${app.auth.frontend-redirect-url:http://localhost:3000}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String googleSub = oauth2User.getAttribute(ATTR_SUB);
        String email     = oauth2User.getAttribute(ATTR_EMAIL);
        String name      = oauth2User.getAttribute(ATTR_NAME);
        String picture   = oauth2User.getAttribute(ATTR_PICTURE);

        if (googleSub == null || email == null) {
            log.error("OAuth2 callback missing required OIDC attributes: sub={} email={}", googleSub, email);
            response.sendRedirect(frontendRedirectUrl + "?error=AUTH_ATTRIBUTES_MISSING");
            return;
        }

        User user   = userService.findOrCreate(googleSub, email, name, picture);
        String jwt  = jwtService.signToken(user.getId(), user.getEmail(), user.getName(), user.getPicture(), user.getRole());

        // Cookie maxAge in seconds; expiry comes from config — never hardcoded
        int maxAgeSeconds = (int) (jwtProperties.accessTokenExpiryMs() / 1000);

        // Cookie.setAttribute("SameSite") available in Servlet 6.0+ (Jakarta EE 10+, Spring Boot 4.x)
        Cookie jwtCookie = new Cookie(JwtAuthFilter.JWT_COOKIE_NAME, jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(maxAgeSeconds);
        jwtCookie.setAttribute("SameSite", "Lax");
        response.addCookie(jwtCookie);

        log.info("OAuth2 login success: userId={} email={}", user.getId(), user.getEmail());
        response.sendRedirect(frontendRedirectUrl);
    }
}
