package com.interviewlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Interview Lab — AI-powered interview mentorship engine.
 *
 * Java 25 LTS | Spring Boot 4.1.0 monolith | Virtual threads enabled globally.
 *
 * Virtual threads: configured via spring.threads.virtual.enabled=true in application.yml.
 * This enables virtual thread execution for Tomcat request handling and all @Async tasks
 * without any additional Java code. No TomcatProtocolHandlerCustomizer needed.
 *
 * @ConfigurationPropertiesScan: registers all @ConfigurationProperties beans (e.g. JwtProperties)
 * discovered on the classpath — no need for @EnableConfigurationProperties per class.
 *
 * Architecture: two-agent system (InterviewAgent + MentorAgent) with AgentTools
 * chain (Chain of Responsibility), AIProvider strategy (sealed interface, Java 25),
 * and in-process EventPublisher (V1) → Kafka (V2).
 *
 * @author Shivendra Mohan Raina
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class InterviewLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewLabApplication.class, args);
    }
}
