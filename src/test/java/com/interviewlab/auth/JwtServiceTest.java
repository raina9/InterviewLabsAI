package com.interviewlab.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties properties;

    // 64 zero-bytes, base64-encoded — valid 512-bit key, safe for tests only
    private static final String TEST_SECRET =
        Base64.getEncoder().encodeToString(new byte[64]);

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(TEST_SECRET, 86400000L, 2592000000L, "test-issuer");
        jwtService = new JwtService(properties);
    }

    @Test
    void signToken_producesNonBlankJwt() {
        String token = jwtService.signToken(UUID.randomUUID(), "user@example.com", "Test User", null);
        assertThat(token).isNotBlank();
        // JWT format: three base64url segments separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void verifyToken_validToken_returnsClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.signToken(userId, "user@example.com", "Test User", "https://pic.url");

        Claims claims = jwtService.verifyToken(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get(JwtService.CLAIM_EMAIL, String.class)).isEqualTo("user@example.com");
        assertThat(claims.get(JwtService.CLAIM_NAME,  String.class)).isEqualTo("Test User");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
    }

    @Test
    void verifyToken_tamperedToken_throwsAuthTokenInvalid() {
        String token = jwtService.signToken(UUID.randomUUID(), "user@example.com", "Test User", null);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.verifyToken(tampered))
            .isInstanceOf(AuthException.class)
            .satisfies(ex -> assertThat(((AuthException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID));
    }

    @Test
    void verifyToken_expiredToken_throwsAuthTokenExpired() {
        // 1ms expiry — will be expired immediately
        JwtProperties shortLived = new JwtProperties(TEST_SECRET, 1L, 1L, "test-issuer");
        JwtService shortJwtService = new JwtService(shortLived);
        String token = shortJwtService.signToken(UUID.randomUUID(), "u@example.com", "U", null);

        // Sleep to guarantee expiry before parse
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThatThrownBy(() -> shortJwtService.verifyToken(token))
            .isInstanceOf(AuthException.class)
            .satisfies(ex -> assertThat(((AuthException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED));
    }

    @Test
    void extractPrincipal_reconstructsFromClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.signToken(userId, "user@example.com", "Test User", "https://pic.url");
        Claims claims = jwtService.verifyToken(token);

        AuthenticatedUser principal = jwtService.extractPrincipal(claims);

        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.name()).isEqualTo("Test User");
        assertThat(principal.picture()).isEqualTo("https://pic.url");
    }
}
