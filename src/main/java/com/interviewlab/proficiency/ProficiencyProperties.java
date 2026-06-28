package com.interviewlab.proficiency;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.proficiency.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * Injected at the service layer — never hardcoded in the Proficiency entity.
 */
@ConfigurationProperties(prefix = "app.proficiency")
public record ProficiencyProperties(double defaultScore, int defaultSessionsCount) {}
