package com.interviewlab.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerFeedbackRepository extends JpaRepository<AnswerFeedback, UUID> {

    List<AnswerFeedback> findBySessionId(UUID sessionId);
}
