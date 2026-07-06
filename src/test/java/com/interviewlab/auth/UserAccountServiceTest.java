package com.interviewlab.auth;

import com.interviewlab.ai.AiProvider;
import com.interviewlab.feedback.AnswerFeedback;
import com.interviewlab.feedback.AnswerFeedbackRepository;
import com.interviewlab.profile.UserProfile;
import com.interviewlab.profile.UserProfileRepository;
import com.interviewlab.proficiency.Proficiency;
import com.interviewlab.proficiency.ProficiencyRepository;
import com.interviewlab.session.InterviewType;
import com.interviewlab.session.Message;
import com.interviewlab.session.MessageRepository;
import com.interviewlab.session.MessageRole;
import com.interviewlab.session.Session;
import com.interviewlab.session.SessionRepository;
import com.interviewlab.session.SessionStatus;
import com.interviewlab.sessionstore.InMemorySessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full DELETE /me cascade, exercised against real repositories (H2) — proves the
 * answer_feedback -> messages -> proficiency -> sessions -> user_profiles -> users
 * deletion order actually respects every FK in the schema, not just a mocked call chain.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserAccountServiceTest {

    @Autowired UserRepository           userRepository;
    @Autowired SessionRepository        sessionRepository;
    @Autowired MessageRepository        messageRepository;
    @Autowired AnswerFeedbackRepository answerFeedbackRepository;
    @Autowired ProficiencyRepository    proficiencyRepository;
    @Autowired UserProfileRepository    userProfileRepository;
    @Autowired TestEntityManager        entityManager;

    UserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        userAccountService = new UserAccountService(
            userRepository, sessionRepository, messageRepository,
            answerFeedbackRepository, proficiencyRepository, userProfileRepository,
            new InMemorySessionStore()
        );
    }

    // -------------------------------------------------------------------------
    // Scenario 1: full flow — data across every table is gone after deletion
    // -------------------------------------------------------------------------

    @Test
    void deleteAccount_removesAllDataAcrossEveryTable() {
        User user = entityManager.persistAndFlush(
            new User("google-sub-delete-me", "delete-me@example.com", "Delete Me", null)
        );
        UUID userId = user.getId();

        Session session = entityManager.persistAndFlush(new Session(
            userId, InterviewType.TECHNICAL, "Senior Engineer", "Sample JD", "MEDIUM", SessionStatus.ACTIVE
        ));
        Message message = entityManager.persistAndFlush(
            new Message(session.getId(), MessageRole.INTERVIEWER, "Tell me about yourself.", 1, false)
        );
        entityManager.persistAndFlush(new AnswerFeedback(
            session.getId(), message.getId(), "Q1", "My answer", "Refined", "Model", 7,
            "Good structure", "Add more depth", "Confident tone"
        ));
        entityManager.persistAndFlush(new Proficiency(userId, "Java", 6.0, 3));
        entityManager.persistAndFlush(new UserProfile(userId, AiProvider.OLLAMA));

        userAccountService.deleteAccount(userId);
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(sessionRepository.findByUserId(userId)).isEmpty();
        assertThat(messageRepository.findBySessionIdOrderBySequence(session.getId())).isEmpty();
        assertThat(answerFeedbackRepository.findBySessionId(session.getId())).isEmpty();
        assertThat(proficiencyRepository.findByUserId(userId)).isEmpty();
        assertThat(userProfileRepository.findByUserId(userId)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: user with no sessions/profile at all — still deletes cleanly
    // -------------------------------------------------------------------------

    @Test
    void deleteAccount_userWithNoOtherData_deletesCleanly() {
        User user = entityManager.persistAndFlush(
            new User("google-sub-bare", "bare@example.com", "Bare User", null)
        );
        UUID userId = user.getId();

        userAccountService.deleteAccount(userId);
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: unknown user — USER_NOT_FOUND, nothing touched
    // -------------------------------------------------------------------------

    @Test
    void deleteAccount_unknownUser_throwsUserNotFound() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> userAccountService.deleteAccount(randomId))
            .isInstanceOf(AuthException.class)
            .satisfies(ex -> assertThat(((AuthException) ex).errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // Scenario 4: calling delete again after the account is gone — proves the JWT's
    // underlying identity genuinely no longer resolves, not just that the first call worked
    // -------------------------------------------------------------------------

    @Test
    void deleteAccount_calledAgainAfterDeletion_throwsUserNotFound() {
        User user = entityManager.persistAndFlush(
            new User("google-sub-twice", "twice@example.com", "Twice Deleted", null)
        );
        UUID userId = user.getId();

        userAccountService.deleteAccount(userId);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> userAccountService.deleteAccount(userId))
            .isInstanceOf(AuthException.class)
            .satisfies(ex -> assertThat(((AuthException) ex).errorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
