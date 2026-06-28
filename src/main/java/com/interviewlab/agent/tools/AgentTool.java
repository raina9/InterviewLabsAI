package com.interviewlab.agent.tools;

/**
 * Chain of Responsibility link — each tool enriches the agent context independently.
 * Tools are stateless: each execute() call fetches fresh data from its data source.
 */
public interface AgentTool {

    String name();

    String execute(AgentContext context);
}
