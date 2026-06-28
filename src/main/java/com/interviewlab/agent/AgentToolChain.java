package com.interviewlab.agent;

import com.interviewlab.agent.tools.AgentContext;
import com.interviewlab.agent.tools.AgentTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chain of Responsibility executor — runs all AgentTool beans in @Order sequence.
 * Results keyed by tool name. A failing tool logs a warning and contributes empty string —
 * the chain never aborts on a single tool failure.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AgentToolChain {

    private final List<AgentTool> tools;

    public Map<String, String> execute(AgentContext context) {
        Map<String, String> results = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            try {
                results.put(tool.name(), tool.execute(context));
            } catch (Exception ex) {
                log.warn("AgentTool '{}' failed — contributing empty: {}", tool.name(), ex.getMessage());
                results.put(tool.name(), "");
            }
        }
        return results;
    }
}
