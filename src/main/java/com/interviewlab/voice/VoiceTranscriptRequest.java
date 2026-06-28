package com.interviewlab.voice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VoiceTranscriptRequest(
    @NotNull UUID sessionId,
    @NotBlank String transcript
) {}
