package com.interviewlab.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerFeedbackRepository extends JpaRepository<AnswerFeedback, UUID> {

    List<AnswerFeedback> findBySessionId(UUID sessionId);

    // Account deletion cascade (see UserAccountService) — answer_feedback has no
    // ON DELETE CASCADE to sessions/messages, so it must be deleted first, explicitly.
    void deleteBySessionIdIn(List<UUID> sessionIds);
}
