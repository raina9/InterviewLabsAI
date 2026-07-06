package com.interviewlab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.deployment.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * mode=personal: single user, relaxed limits, in-memory store allowed.
 * mode=production: multi-user/multi-instance — DeploymentModeValidator enforces
 * hard prerequisites (AUTH_MODE=oauth, REDIS_URL configured) at startup.
 */
@ConfigurationProperties(prefix = "app.deployment")
public record DeploymentProperties(String mode) {

    public boolean isProduction() {
        return "production".equalsIgnoreCase(mode);
    }
}
