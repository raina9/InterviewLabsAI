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

    private final DeploymentProperties deploymentProperties;
    private final AuthProperties authProperties;
    private final String redisUrl;
    private final String aiProvider;

    public DeploymentModeValidator(
            DeploymentProperties deploymentProperties,
            AuthProperties authProperties,
            @Value("${spring.data.redis.url:}") String redisUrl,
            @Value("${app.ai.provider:ollama}") String aiProvider) {
        this.deploymentProperties = deploymentProperties;
        this.authProperties = authProperties;
        this.redisUrl = redisUrl;
        this.aiProvider = aiProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionReadiness() {
        if (!deploymentProperties.isProduction()) {
            return;
        }

        if ("dev".equalsIgnoreCase(authProperties.mode())) {
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
}
