package com.interviewlab.agent.orchestration;

import com.interviewlab.agent.tools.AgentContext;

/**
 * Agent orchestration contract — decouples the "which tools to run" decision
 * from the "how to run them" implementation.
 *
 * Local: AgentToolChain runs all tools in-process (zero infra, default).
 * Cloud: a future CloudAgentOrchestrator delegates to a managed orchestration
 *        platform (e.g. LangChain, Vertex AI Agents, AWS Bedrock Agents).
 *
 * Switch via AGENT_ORCHESTRATION_MODE env var — no code change required.
 */
public interface AgentOrchestrator {

    /**
     * Run the tool chain for the given agent context and return all tool outputs.
     * Never throws on a single tool failure — degraded output is returned with empty strings.
     */
    OrchestrationResult orchestrate(AgentContext context);
}
