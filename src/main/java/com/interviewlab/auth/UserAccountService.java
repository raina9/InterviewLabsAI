package com.interviewlab.auth;

import com.interviewlab.feedback.AnswerFeedbackRepository;
import com.interviewlab.proficiency.ProficiencyRepository;
import com.interviewlab.profile.UserProfileRepository;
import com.interviewlab.session.MessageRepository;
import com.interviewlab.session.Session;
import com.interviewlab.session.SessionRepository;
import com.interviewlab.sessionstore.SessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full account deletion (GDPR-style right to erasure).
 * Deliberately a dedicated service, not folded into UserService — cascading across
 * six tables/five other packages is a distinct responsibility from identity lookup/upsert.
 *
 * Deletion order respects FK constraints, deleting explicitly rather than relying solely
 * on DB-level ON DELETE CASCADE (only user_profiles->users and messages->sessions have
 * that; sessions->users, answer_feedback->sessions/messages, and proficiency->users do not):
 *   answer_feedback -> messages -> proficiency -> sessions -> user_profiles -> users
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserAccountService {

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";

    private final UserRepository           userRepository;
    private final SessionRepository        sessionRepository;
    private final MessageRepository        messageRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final ProficiencyRepository    proficiencyRepository;
    private final UserProfileRepository    userProfileRepository;
    private final SessionStore             sessionStore;

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(
                ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND,
                "User with id " + userId + " not found"
            ));

        try {
            List<Session> sessions = sessionRepository.findByUserId(userId);
            List<UUID> sessionIds = sessions.stream().map(Session::getId).toList();

            if (!sessionIds.isEmpty()) {
                answerFeedbackRepository.deleteBySessionIdIn(sessionIds);
                messageRepository.deleteBySessionIdIn(sessionIds);
            }
            proficiencyRepository.deleteByUserId(userId);
            sessionRepository.deleteAll(sessions);
            userProfileRepository.deleteById(userId);
            userRepository.delete(user);

            // Best-effort only: quiz/drill/code SessionStore sessions carry no userId today
            // (pre-existing gap, out of scope here) and are left to expire via their own
            // TTL. The rate-limit counter IS genuinely keyed by userId, so it's cleared.
            sessionStore.delete(rateLimitKey(userId));

            log.info("Account deleted: userId={}", userId);
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Account deletion failed: userId={}", userId, ex);
            throw new AuthException(
                ErrorCode.USER_DELETE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete your account. Please try again or contact support."
            );
        }
    }

    private String rateLimitKey(UUID userId) {
        return RATE_LIMIT_KEY_PREFIX + userId + ":" + LocalDate.now();
    }
}
