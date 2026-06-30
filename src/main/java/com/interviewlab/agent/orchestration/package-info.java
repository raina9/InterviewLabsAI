/**
 * Agent orchestration abstraction — Swappable Backend Pattern.
 *
 * Active (default): LocalAgentOrchestrator
 *   Condition: @ConditionalOnProperty(name="app.agent.orchestration-mode", havingValue="local", matchIfMissing=true)
 *   Config:    AGENT_ORCHESTRATION_MODE=local (or unset)
 *   Behaviour: Runs AgentToolChain in-process. Zero infra dependency.
 *   Cost:      zero
 *
 * Future (cloud): CloudAgentOrchestrator (not yet built — seam is ready)
 *   Condition: @ConditionalOnProperty(name="app.agent.orchestration-mode", havingValue="cloud")
 *   Config:    AGENT_ORCHESTRATION_MODE=cloud + cloud platform credentials
 *   Activation steps:
 *     1. Create CloudAgentOrchestrator implementing AgentOrchestrator
 *     2. Annotate with @Component @ConditionalOnProperty(name="app.agent.orchestration-mode", havingValue="cloud")
 *     3. Add the cloud platform SDK dependency to pom.xml (commented, ready to uncomment)
 *     4. Set AGENT_ORCHESTRATION_MODE=cloud in environment
 *   Candidates: LangChain4j, AWS Bedrock Agents, Vertex AI Agents
 *
 * Callers (InterviewAgent, MentorAgent) inject AgentOrchestrator — not the concrete class.
 * The bean swap is transparent to all callers.
 */
package com.interviewlab.agent.orchestration;
