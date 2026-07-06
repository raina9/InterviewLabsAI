package com.interviewlab.feedback;

import com.interviewlab.auth.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class SystemFeedbackService {

    private final SystemFeedbackRepository systemFeedbackRepository;

    @Transactional
    public void setApplied(UUID id, boolean applied) {
        SystemFeedback feedback = systemFeedbackRepository.findById(id)
            .orElseThrow(() -> new FeedbackException(
                ErrorCode.SYSTEM_FEEDBACK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "System feedback " + id + " not found."
            ));
        feedback.setApplied(applied);
        log.info("System feedback applied flag updated: id={} applied={}", id, applied);
    }
}
