package com.interviewlab.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.agent.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * All agent prompts and parameters from env vars — never hardcoded in agent classes.
 */
@ConfigurationProperties(prefix = "app.agent")
public record AgentProperties(
    int    historyWindowSize,
    int    totalQuestions,
    String interviewSystemPrompt,
    String mentorSystemPrompt,
    String followUpInstruction,
    String moveOnInstruction
) {}
