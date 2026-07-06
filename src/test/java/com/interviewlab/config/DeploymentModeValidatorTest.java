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

    // -------------------------------------------------------------------------
    // Scenario 1: mode=personal — no checks run, even with dev auth and no Redis
    // -------------------------------------------------------------------------

    @Test
    void validate_personalMode_neverThrows() {
        DeploymentModeValidator validator = new DeploymentModeValidator(
            new DeploymentProperties("personal"), authProperties("dev"), "", "ollama"
        );

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: mode=production + AUTH_MODE=dev — fails startup
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithDevAuth_throws() {
        DeploymentModeValidator validator = new DeploymentModeValidator(
            new DeploymentProperties("production"), authProperties("dev"), "redis://localhost:6379", "gemini"
        );

        assertThatThrownBy(validator::validateProductionReadiness)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AUTH_MODE=oauth");
    }

    // -------------------------------------------------------------------------
    // Scenario 3: mode=production + no REDIS_URL — fails startup
    // -------------------------------------------------------------------------

    @Test
    void validate_productionWithoutRedisUrl_throws() {
        DeploymentModeValidator validator = new DeploymentModeValidator(
            new DeploymentProperties("production"), authProperties("oauth"), "", "gemini"
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
        DeploymentModeValidator validator = new DeploymentModeValidator(
            new DeploymentProperties("production"), authProperties("oauth"), "redis://localhost:6379", "ollama"
        );

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: mode=production + all prerequisites satisfied — passes cleanly
    // -------------------------------------------------------------------------

    @Test
    void validate_productionFullyConfigured_passes() {
        DeploymentModeValidator validator = new DeploymentModeValidator(
            new DeploymentProperties("production"), authProperties("oauth"), "redis://localhost:6379", "gemini"
        );

        assertThatCode(validator::validateProductionReadiness).doesNotThrowAnyException();
    }
}
