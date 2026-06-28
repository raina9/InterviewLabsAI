package com.interviewlab.session;

import com.interviewlab.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = entityManager.persistAndFlush(
            new User("google-sub-session-test", "session-test@example.com", "Session Test User", null)
        );
    }

    @Test
    void findByUserId_returnsAllSessionsForUser() {
        entityManager.persistAndFlush(new Session(
            user.getId(), InterviewType.TECHNICAL, "Senior Engineer", "Sample JD", "MEDIUM", SessionStatus.ACTIVE));
        entityManager.persistAndFlush(new Session(
            user.getId(), InterviewType.HR, "Product Manager", "Another JD", "EASY", SessionStatus.COMPLETED));

        List<Session> sessions = sessionRepository.findByUserId(user.getId());

        assertThat(sessions).hasSize(2);
    }

    @Test
    void findByUserId_returnsEmpty_whenUserHasNoSessions() {
        List<Session> sessions = sessionRepository.findByUserId(user.getId());

        assertThat(sessions).isEmpty();
    }

    @Test
    void findByUserIdAndStatus_returnsOnlyMatchingStatus() {
        entityManager.persistAndFlush(new Session(
            user.getId(), InterviewType.TECHNICAL, "Senior Engineer", "Sample JD", "MEDIUM", SessionStatus.ACTIVE));
        entityManager.persistAndFlush(new Session(
            user.getId(), InterviewType.BEHAVIOURAL, "Team Lead", "Behavioural JD", "HARD", SessionStatus.COMPLETED));
        entityManager.persistAndFlush(new Session(
            user.getId(), InterviewType.SYSTEM_DESIGN, "Staff Engineer", "SD JD", "HARD", SessionStatus.ABANDONED));

        List<Session> active = sessionRepository.findByUserIdAndStatus(user.getId(), SessionStatus.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(active.get(0).getInterviewType()).isEqualTo(InterviewType.TECHNICAL);
    }

    @Test
    void findByUserIdAndStatus_returnsEmpty_whenNoMatchingStatus() {
        entityManager.persistAndFlush(new Session(
            user.getId(), InterviewType.TECHNICAL, "Senior Engineer", "Sample JD", "MEDIUM", SessionStatus.ACTIVE));

        List<Session> abandoned = sessionRepository.findByUserIdAndStatus(user.getId(), SessionStatus.ABANDONED);

        assertThat(abandoned).isEmpty();
    }

    @Test
    void findByUserIdAndStatus_doesNotReturnOtherUsersessions() {
        User otherUser = entityManager.persistAndFlush(
            new User("google-sub-other", "other@example.com", "Other User", null)
        );
        entityManager.persistAndFlush(new Session(
            otherUser.getId(), InterviewType.TECHNICAL, "Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE));

        List<Session> sessions = sessionRepository.findByUserIdAndStatus(user.getId(), SessionStatus.ACTIVE);

        assertThat(sessions).isEmpty();
    }
}
