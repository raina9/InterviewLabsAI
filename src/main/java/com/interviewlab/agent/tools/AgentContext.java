package com.interviewlab.agent.tools;

import java.util.UUID;

/**
 * Immutable context threaded through the AgentToolChain per agent invocation.
 * messageId and candidateAnswer are nullable — present only in mentor-loop cycles.
 */
public record AgentContext(
    UUID   userId,
    UUID   sessionId,
    UUID   messageId,
    String candidateAnswer
) {}
