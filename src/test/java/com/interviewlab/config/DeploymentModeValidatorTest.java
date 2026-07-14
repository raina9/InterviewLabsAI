package com.interviewlab.config;

import com.interviewlab.auth.AuthProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentModeValidatorTest {

    private static final UUID DEV_USER_ID = UUID.randomUUID();

    private AuthProperties authProperties(String mode) {
        return new AuthProperties(mode, true, "dev-secret", DEV_USER_ID, "dev@interviewlab.local", "Dev User", "http://localhost:3000");
    }

    private AuthProperties authProperties(String mode, boolean autoDetect) {
        return new AuthProperties(mode, autoDetect, "dev-secret", DEV_USER_ID, "dev@interviewlab.local", "Dev User", "http://localhost:3000");
    }

    private DeploymentModeValidator validator(
            String deploymentMode, AuthProperties authProperties, String redisUrl, String aiProvider) {
        return new DeploymentModeValidator(
            new DeploymentProperties(deploymentMode), authProperties, redisUrl, aiProvider,
            "not-configured", "not-configured"
        );
    }

    private DeploymentModeValidator validator(
            String deploymentMode, AuthProperties authProperties, String redisUrl, String aiProvider,
            String googleClientId, String googleClientSecret) {
        return new DeploymentModeValidator(
            new DeploymentProperties(deploymentMode), authProperties, redisUrl, aiProvider,
            googleClientId, googleClientSecret
        );
    }

    // -------------------------------------------------------------------------
    // Scenario 1: mode=personal — no checks run, even with dev auth and no Redis
    // -------------------------------------------------------------------------

    @Test
    void validate_personalMode_neverThrows() {
        DeploymentModeValidator validator = validator("personal", authProperties("dev"), "", "ollama");

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: mode=production + AUTH_MODE=dev — fails startup, even if Google
    // credentials happen to be present (explicit dev must still win, see AuthProperties)
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithDevAuth_throws() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("dev"), "redis://localhost:6379", "gemini"
        );

        assertThatThrownBy(validator::validateProductionReadiness)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AUTH_MODE=oauth");
    }

    @Test
    void validate_productionWithExplicitDevAuth_throwsEvenWithGoogleCredentialsPresent() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("dev"), "redis://localhost:6379", "gemini",
            "real-client-id", "real-client-secret"
        );

        assertThatThrownBy(validator::validateProductionReadiness)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AUTH_MODE=oauth");
    }

    // -------------------------------------------------------------------------
    // Scenario 2b: mode=production + AUTH_MODE blank + no Google credentials —
    // auto-detect resolves to dev, guard must still fire (this is the gap the
    // old raw "dev".equalsIgnoreCase(mode) check would have missed)
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithBlankAuthModeAndNoCredentials_throws() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("", true), "redis://localhost:6379", "gemini"
        );

        assertThatThrownBy(validator::validateProductionReadiness)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AUTH_MODE=oauth");
    }

    // -------------------------------------------------------------------------
    // Scenario 2c: mode=production + AUTH_MODE blank + real Google credentials —
    // auto-detect resolves to oauth, guard passes the auth check
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithBlankAuthModeAndGoogleCredentials_passesAuthCheck() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("", true), "redis://localhost:6379", "gemini",
            "real-client-id", "real-client-secret"
        );

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: mode=production + no REDIS_URL — fails startup
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithoutRedisUrl_throws() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("oauth"), "", "gemini"
        );

        assertThatThrownBy(validator::validateProductionReadiness)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("REDIS_URL");
    }

    // -------------------------------------------------------------------------
    // Scenario 4: mode=production + AI_PROVIDER=ollama — allowed, just a warning
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithOllama_allowedButLogsWarning() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("oauth"), "redis://localhost:6379", "ollama"
        );

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: mode=production + all prerequisites satisfied — passes cleanly
    // -------------------------------------------------------------------------

    @Test
    void validate_productionFullyConfigured_passes() {
        DeploymentModeValidator validator = validator(
            "production", authProperties("oauth"), "redis://localhost:6379", "gemini"
        );

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }
}
