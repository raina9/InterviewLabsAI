package com.interviewlab.auth;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the AUTH_MODE precedence bug: a leftover Google credential in .env must never
 * silently flip AUTH_MODE=dev into oauth. Explicit mode always wins; auto-detect only
 * applies when mode is blank/unset. See SecurityConfig.useOAuth and
 * DeploymentModeValidator, both of which delegate to isOAuthEffective.
 */
class AuthPropertiesTest {

    private static final UUID DEV_USER_ID = UUID.randomUUID();

    private AuthProperties authProperties(String mode, boolean autoDetect) {
        return new AuthProperties(
            mode, autoDetect, "dev-secret", DEV_USER_ID,
            "dev@interviewlab.local", "Dev User", "http://localhost:3000"
        );
    }

    @Test
    void explicitDevMode_winsOverAutoDetect_evenWithGoogleCredentialsPresent() {
        AuthProperties props = authProperties("dev", true);

        assertThat(props.isOAuthEffective(true)).isFalse();
    }

    @Test
    void explicitOAuthMode_winsOverAutoDetect_evenWithoutGoogleCredentials() {
        AuthProperties props = authProperties("oauth", false);

        assertThat(props.isOAuthEffective(false)).isTrue();
    }

    @Test
    void blankMode_withGoogleCredentials_autoDetectsOAuth() {
        AuthProperties props = authProperties("", true);

        assertThat(props.isOAuthEffective(true)).isTrue();
    }

    @Test
    void blankMode_withoutGoogleCredentials_autoDetectsDev() {
        AuthProperties props = authProperties("", true);

        assertThat(props.isOAuthEffective(false)).isFalse();
    }

    @Test
    void blankMode_withAutoDetectDisabled_defaultsToDevRegardlessOfCredentials() {
        AuthProperties props = authProperties("", false);

        assertThat(props.isOAuthEffective(true)).isFalse();
    }

    @Test
    void nullMode_treatedSameAsBlank_autoDetectApplies() {
        AuthProperties props = authProperties(null, true);

        assertThat(props.isOAuthEffective(true)).isTrue();
        assertThat(props.isOAuthEffective(false)).isFalse();
    }

    @Test
    void modeIsCaseInsensitive() {
        assertThat(authProperties("DEV", true).isOAuthEffective(true)).isFalse();
        assertThat(authProperties("OAuth", false).isOAuthEffective(false)).isTrue();
    }
}
