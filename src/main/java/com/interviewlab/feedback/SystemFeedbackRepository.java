package com.interviewlab.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SystemFeedbackRepository extends JpaRepository<SystemFeedback, UUID> {

    List<SystemFeedback> findByUserId(UUID userId);
}
