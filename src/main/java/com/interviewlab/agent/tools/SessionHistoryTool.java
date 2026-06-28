package com.interviewlab.agent.tools;

import com.interviewlab.agent.AgentProperties;
import com.interviewlab.session.Message;
import com.interviewlab.session.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Order(2)
public class SessionHistoryTool implements AgentTool {

    private final MessageRepository  messageRepository;
    private final AgentProperties    agentProperties;

    @Override
    public String name() {
        return "history";
    }

    @Override
    public String execute(AgentContext context) {
        List<Message> messages = messageRepository.findBySessionIdOrderBySequence(context.sessionId());
        int window = agentProperties.historyWindowSize();
        if (messages.size() > window) {
            messages = messages.subList(messages.size() - window, messages.size());
        }
        return messages.stream()
            .map(m -> m.getRole() + ": " + m.getContent())
            .collect(Collectors.joining("\n"));
    }
}
