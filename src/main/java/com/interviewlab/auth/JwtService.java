package com.interviewlab.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT creation and validation service.
 *
 * Algorithm: JJWT auto-selects HS512 for a 64-byte key (stronger than the HS256
 * minimum requirement — "enhance always" rule applies here).
 * Key: base64-decoded JWT_SECRET env var. Must be >= 64 bytes.
 *
 * Claims packed into every token: sub (userId), email, name, picture.
 * Storing name + picture avoids a DB round-trip on every request to /me.
 */
@Slf4j
@Service
public class JwtService {

    static final String CLAIM_EMAIL   = "email";
    static final String CLAIM_NAME    = "name";
    static final String CLAIM_PICTURE = "picture";

    private final SecretKey secretKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Base64.getDecoder().decode(properties.secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Issues a signed JWT access token.
     * Sub = userId (stable identity). Claims include display fields for the /me endpoint.
     */
    public String signToken(UUID userId, String email, String name, String picture) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + properties.accessTokenExpiryMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_EMAIL,   email)
                .claim(CLAIM_NAME,    name)
                .claim(CLAIM_PICTURE, picture)
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)   // JJWT picks HS512 for 64-byte key
                .compact();
    }

    /**
     * Validates a JWT and returns its claims.
     * Throws AuthException (never returns null) — callers can trust the result is valid.
     */
    public Claims verifyToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
            throw new AuthException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new AuthException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    /**
     * Reconstructs the security principal from validated claims.
     * Called only after verifyToken() — claims are guaranteed valid here.
     */
    public AuthenticatedUser extractPrincipal(Claims claims) {
        return new AuthenticatedUser(
            UUID.fromString(claims.getSubject()),
            claims.get(CLAIM_EMAIL,   String.class),
            claims.get(CLAIM_NAME,    String.class),
            claims.get(CLAIM_PICTURE, String.class)
        );
    }
}
