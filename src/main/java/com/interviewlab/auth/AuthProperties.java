package com.interviewlab.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * Bound from app.auth.* in application.yml.
 * mode=dev: DevTokenFilter active, oauth2Login disabled.
 * mode=oauth: JwtAuthFilter + oauth2Login active, DevTokenFilter skipped.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    String mode,
    String devToken,
    UUID   devUserId,
    String devUserEmail,
    String devUserName,
    String frontendRedirectUrl
) {}
