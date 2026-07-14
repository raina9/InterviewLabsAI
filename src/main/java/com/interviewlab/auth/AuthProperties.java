package com.interviewlab.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * Bound from app.auth.* in application.yml.
 * mode=dev: DevTokenFilter active, oauth2Login disabled.
 * mode=oauth: JwtAuthFilter + oauth2Login active, DevTokenFilter skipped.
 * mode blank/unset: auto-detect decides — see {@link #isOAuthEffective}.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    String  mode,
    boolean autoDetect,
    String  devToken,
    UUID    devUserId,
    String  devUserEmail,
    String  devUserName,
    String  frontendRedirectUrl
) {

    /**
     * Explicit AUTH_MODE always wins over auto-detection — a leftover Google credential
     * in .env must never silently flip AUTH_MODE=dev into oauth. Auto-detect only applies
     * when mode is blank/unset. Single source of truth for SecurityConfig (filter wiring)
     * and DeploymentModeValidator (production guard) so both agree on the effective mode.
     */
    public boolean isOAuthEffective(boolean hasGoogleCredentials) {
        if ("oauth".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("dev".equalsIgnoreCase(mode)) {
            return false;
        }
        return autoDetect && hasGoogleCredentials;
    }
}
