package com.interviewlab.ai;

/**
 * Supported AI provider implementations.
 * Stored as TEXT in user_profiles.preferred_ai_provider via @Enumerated(EnumType.STRING).
 * OLLAMA: active (local, zero cost). GEMINI/CLAUDE/OPENAI: parked — unpark trigger: explicit owner instruction.
 */
public enum AiProvider {
    OLLAMA,
    GEMINI,
    CLAUDE,
    OPENAI
}
