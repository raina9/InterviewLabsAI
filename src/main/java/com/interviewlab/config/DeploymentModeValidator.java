package com.interviewlab.config;

import com.interviewlab.auth.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fails startup fast when DEPLOYMENT_MODE=production is declared without its hard
 * prerequisites — catching a misconfigured production deploy before it serves traffic,
 * rather than discovering it via a 3am incident. See ADR-009.
 *
 * personal mode: no checks run — relaxed by design for single-user local/dev use.
 */
@Slf4j
@Component
public class DeploymentModeValidator {

    private static final String NOT_CONFIGURED = "not-configured";

    private final DeploymentProperties deploymentProperties;
    private final AuthProperties authProperties;
    private final String redisUrl;
    private final String aiProvider;
    private final String googleClientId;
    private final String googleClientSecret;

    public DeploymentModeValidator(
            DeploymentProperties deploymentProperties,
            AuthProperties authProperties,
            @Value("${spring.data.redis.url:}") String redisUrl,
            @Value("${app.ai.provider:ollama}") String aiProvider,
            @Value("${GOOGLE_CLIENT_ID:not-configured}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:not-configured}") String googleClientSecret) {
        this.deploymentProperties = deploymentProperties;
        this.authProperties = authProperties;
        this.redisUrl = redisUrl;
        this.aiProvider = aiProvider;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionReadiness() {
        if (!deploymentProperties.isProduction()) {
            return;
        }

        // Mirrors SecurityConfig's filter-wiring decision (via AuthProperties.isOAuthEffective)
        // rather than checking the raw mode string — a blank AUTH_MODE with no Google
        // credentials resolves to dev-token auth just as surely as an explicit AUTH_MODE=dev,
        // and must trip this guard the same way.
        if (!authProperties.isOAuthEffective(hasGoogleCredentials())) {
            throw new IllegalStateException(
                "DEPLOYMENT_MODE=production requires AUTH_MODE=oauth. " +
                "AUTH_MODE=dev bypasses real authentication and is not production-safe."
            );
        }

        if (redisUrl == null || redisUrl.isBlank()) {
            throw new IllegalStateException(
                "DEPLOYMENT_MODE=production requires REDIS_URL to be configured. " +
                "The in-memory session store does not survive multi-instance or multi-restart deploys."
            );
        }

        if ("ollama".equalsIgnoreCase(aiProvider)) {
            log.warn(
                "DEPLOYMENT_MODE=production with AI_PROVIDER=ollama — allowed, but every production " +
                "instance must reach a running Ollama server. Verify OLLAMA_BASE_URL before relying on this."
            );
        }

        log.info("DEPLOYMENT_MODE=production readiness checks passed");
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
