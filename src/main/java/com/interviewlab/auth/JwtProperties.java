package com.interviewlab.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.jwt.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Secret MUST be a base64-encoded 64-byte value (generate: openssl rand -base64 64).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTokenExpiryMs,
    long refreshTokenExpiryMs,
    String issuer
) {}
