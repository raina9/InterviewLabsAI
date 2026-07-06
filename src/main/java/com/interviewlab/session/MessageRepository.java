package com.interviewlab.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySessionIdOrderBySequence(UUID sessionId);

    // Account deletion cascade (see UserAccountService) — deleted explicitly, ahead of
    // sessions, even though messages already has ON DELETE CASCADE from sessions.
    void deleteBySessionIdIn(List<UUID> sessionIds);
}
