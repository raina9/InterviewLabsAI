package com.interviewlab.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Claude provider stub — parked P1.
 * Unpark trigger: explicit owner instruction + cost evaluation.
 * Activation: set AI_PROVIDER=claude — this bean only instantiates when that property matches.
 * Implements AIProvider so the sealed interface remains exhaustive.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "claude")
public final class ClaudeProvider implements AiProviderStrategy {

    @Override
    public String generate(String prompt, AIOptions options) {
        throw new UnsupportedOperationException("Claude provider not yet implemented — parked P1");
    }

    @Override
    public String generateJson(String prompt, AIOptions options) {
        throw new UnsupportedOperationException("Claude provider not yet implemented — parked P1");
    }

    @Override
    public AiProvider providerName() {
        return AiProvider.CLAUDE;
    }
}
