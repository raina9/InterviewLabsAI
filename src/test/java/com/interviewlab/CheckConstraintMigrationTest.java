package com.interviewlab;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Executes the real V10__add_check_constraints.sql content (read from classpath,
 * not a hand-copied version) against a minimal H2 schema covering only the columns
 * it touches. A full V1-V10 Flyway run against H2 isn't possible in this suite —
 * H2 doesn't support TIMESTAMPTZ / gen_random_uuid() even in PostgreSQL mode, which
 * is exactly why the project already disables Flyway for tests (see
 * InterviewLabApplicationTests) in favour of Hibernate ddl-auto=create-drop.
 * This test instead proves the migration's actual SQL: dirty data survives the
 * clean step, then the constraint is genuinely enforced afterward.
 */
class CheckConstraintMigrationTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        // Unique DB name per test run (no DB_CLOSE_DELAY) — each test method gets an
        // isolated in-memory instance that's gone the moment this single connection closes.
        String dbName = "check_constraint_test_" + System.nanoTime();
        connection = DriverManager.getConnection("jdbc:h2:mem:" + dbName, "sa", "");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE sessions (id INT PRIMARY KEY, status VARCHAR(20) NOT NULL)");
            stmt.execute("CREATE TABLE answer_feedback (id INT PRIMARY KEY, score INT NOT NULL)");
            stmt.execute("CREATE TABLE proficiency (id INT PRIMARY KEY, score DOUBLE NOT NULL, sessions_count INT NOT NULL)");

            // Dirty data the migration must clean before constraining
            stmt.execute("INSERT INTO sessions VALUES (1, 'ACTIVE'), (2, 'WEIRD_LEGACY_STATE'), (3, 'COMPLETED')");
            stmt.execute("INSERT INTO answer_feedback VALUES (1, 5), (2, -3), (3, 99)");
            stmt.execute("INSERT INTO proficiency VALUES (1, 5.0, 2), (2, -1.0, -4), (3, 15.0, 3)");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void migration_cleansDirtyDataThenConstrainsSuccessfully() throws SQLException, IOException {
        runMigrationScript();

        try (Statement stmt = connection.createStatement()) {
            // Step 1/2: dirty session status cleaned to ABANDONED, valid rows untouched
            var rs = stmt.executeQuery("SELECT id, status FROM sessions ORDER BY id");
            rs.next(); assertThat(rs.getString("status")).isEqualTo("ACTIVE");
            rs.next(); assertThat(rs.getString("status")).isEqualTo("ABANDONED");
            rs.next(); assertThat(rs.getString("status")).isEqualTo("COMPLETED");

            // Step 3/4: out-of-range scores clamped into [0, 10]
            rs = stmt.executeQuery("SELECT id, score FROM answer_feedback ORDER BY id");
            rs.next(); assertThat(rs.getInt("score")).isEqualTo(5);
            rs.next(); assertThat(rs.getInt("score")).isEqualTo(0);
            rs.next(); assertThat(rs.getInt("score")).isEqualTo(10);

            // Step 5: proficiency score clamped to [0, 10], sessions_count clamped to >= 0
            rs = stmt.executeQuery("SELECT id, score, sessions_count FROM proficiency ORDER BY id");
            rs.next(); assertThat(rs.getDouble("score")).isEqualTo(5.0); assertThat(rs.getInt("sessions_count")).isEqualTo(2);
            rs.next(); assertThat(rs.getDouble("score")).isEqualTo(0.0); assertThat(rs.getInt("sessions_count")).isEqualTo(0);
            rs.next(); assertThat(rs.getDouble("score")).isEqualTo(10.0); assertThat(rs.getInt("sessions_count")).isEqualTo(3);
        }
    }

    @Test
    void migration_thenConstraintsActuallyEnforced() throws SQLException, IOException {
        runMigrationScript();

        try (Statement stmt = connection.createStatement()) {
            assertThatThrownBy(() ->
                stmt.execute("INSERT INTO sessions VALUES (4, 'NOT_A_REAL_STATUS')")
            ).isInstanceOf(SQLException.class);

            assertThatThrownBy(() ->
                stmt.execute("INSERT INTO answer_feedback VALUES (4, 11)")
            ).isInstanceOf(SQLException.class);

            assertThatThrownBy(() ->
                stmt.execute("INSERT INTO proficiency VALUES (4, 5.0, -1)")
            ).isInstanceOf(SQLException.class);
        }
    }

    private void runMigrationScript() throws IOException, SQLException {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V10__add_check_constraints.sql")) {
            assertThat(in).as("V10__add_check_constraints.sql must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (Statement stmt = connection.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = Arrays.stream(statement.split("\n"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("")
                    .trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }
}
