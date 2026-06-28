package com.interviewlab.ai;

import org.springframework.stereotype.Component;

/**
 * OpenAI provider stub — parked P1.
 * Unpark trigger: explicit owner instruction + cost evaluation.
 * Implements AIProvider so the sealed interface remains exhaustive.
 */
@Component
public final class OpenAIProvider implements AiProviderStrategy {

    @Override
    public String generate(String prompt, AIOptions options) {
        throw new UnsupportedOperationException("OpenAI provider not yet implemented — parked P1");
    }

    @Override
    public String generateJson(String prompt, AIOptions options) {
        throw new UnsupportedOperationException("OpenAI provider not yet implemented — parked P1");
    }

    @Override
    public AiProvider providerName() {
        return AiProvider.OPENAI;
    }
}
