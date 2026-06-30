package com.interviewlab.agent.orchestration;

import java.util.Map;

/**
 * Output of the agent orchestration step — tool outputs keyed by tool name.
 * Wrapping Map<String,String> in a record preserves the seam for richer outputs
 * in future: trace IDs, token cost, audit log, latency per tool.
 */
public record OrchestrationResult(Map<String, String> toolOutputs) {}
