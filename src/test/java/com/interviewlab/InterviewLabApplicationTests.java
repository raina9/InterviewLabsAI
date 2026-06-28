package com.interviewlab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring context load verification.
 * Uses the "test" profile (application-test.yml) — H2 in-memory, Flyway disabled.
 * Production integration tests use TestContainers with real PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class InterviewLabApplicationTests {

    @Test
    void contextLoads() {
    }
}
