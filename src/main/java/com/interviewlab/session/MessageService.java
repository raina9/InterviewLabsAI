package com.interviewlab.session;

import com.interviewlab.auth.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public Message addMessage(UUID sessionId, MessageRole role, String content, boolean voiceUsed) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new SessionException(
                ErrorCode.SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Session " + sessionId + " not found"
            );
        }
        // V1: count via list size — acceptable for personal/single-instance mode.
        // V2: replace with COUNT query when concurrent writes are possible.
        int nextSequence = messageRepository.findBySessionIdOrderBySequence(sessionId).size() + 1;
        Message message = new Message(sessionId, role, content, nextSequence, voiceUsed);
        Message saved = messageRepository.save(message);
        log.debug("Message added: sessionId={} sequence={} role={}", sessionId, nextSequence, role);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Message> getSessionMessages(UUID sessionId) {
        return messageRepository.findBySessionIdOrderBySequence(sessionId);
    }
}
