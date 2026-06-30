package com.interviewlab.agent.orchestration;

import com.interviewlab.agent.AgentToolChain;
import com.interviewlab.agent.tools.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-process orchestration — delegates to AgentToolChain (Chain of Responsibility).
 * Active by default (AGENT_ORCHESTRATION_MODE=local or unset).
 *
 * Swap to CloudAgentOrchestrator by setting AGENT_ORCHESTRATION_MODE=cloud.
 * See orchestration/package-info.java for the full activation path.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "app.agent.orchestration-mode", havingValue = "local", matchIfMissing = true)
public class LocalAgentOrchestrator implements AgentOrchestrator {

    private final AgentToolChain toolChain;

    @Override
    public OrchestrationResult orchestrate(AgentContext context) {
        log.debug("orchestration.mode=local sessionId={}", context.sessionId());
        return new OrchestrationResult(toolChain.execute(context));
    }
}
