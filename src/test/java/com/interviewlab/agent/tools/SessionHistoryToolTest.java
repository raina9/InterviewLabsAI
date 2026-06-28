package com.interviewlab.agent.tools;

import com.interviewlab.agent.AgentProperties;
import com.interviewlab.session.Message;
import com.interviewlab.session.MessageRepository;
import com.interviewlab.session.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionHistoryToolTest {

    @Mock MessageRepository messageRepository;

    SessionHistoryTool tool;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    AgentProperties agentProperties = new AgentProperties(
        3,                           // historyWindowSize = 3 for tests
        5,                           // totalQuestions
        "Interview prompt",
        "Mentor prompt",
        "Follow up.",
        "Move on."
    );

    @BeforeEach
    void setUp() {
        tool = new SessionHistoryTool(messageRepository, agentProperties);
    }

    private AgentContext ctx() {
        return new AgentContext(USER_ID, SESSION_ID, null, null);
    }

    @Test
    void execute_messagesExist_formatsAsRoleContent() {
        Message q = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Tell me about yourself.", 1, false);
        Message a = new Message(SESSION_ID, MessageRole.CANDIDATE,   "I have 5 years experience.", 2, false);
        when(messageRepository.findBySessionIdOrderBySequence(SESSION_ID)).thenReturn(List.of(q, a));

        String result = tool.execute(ctx());

        assertThat(result).contains("INTERVIEWER: Tell me about yourself.")
                          .contains("CANDIDATE: I have 5 years experience.");
    }

    @Test
    void execute_emptySession_returnsEmpty() {
        when(messageRepository.findBySessionIdOrderBySequence(SESSION_ID)).thenReturn(List.of());

        String result = tool.execute(ctx());

        assertThat(result).isEmpty();
    }

    @Test
    void execute_moreMessagesThanWindow_returnsOnlyLastN() {
        Message m1 = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Q1", 1, false);
        Message m2 = new Message(SESSION_ID, MessageRole.CANDIDATE,   "A1", 2, false);
        Message m3 = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Q2", 3, false);
        Message m4 = new Message(SESSION_ID, MessageRole.CANDIDATE,   "A2", 4, false);
        when(messageRepository.findBySessionIdOrderBySequence(SESSION_ID)).thenReturn(List.of(m1, m2, m3, m4));

        String result = tool.execute(ctx());

        // window size is 3, so only last 3 messages (m2, m3, m4)
        assertThat(result).doesNotContain("Q1")
                          .contains("A1")
                          .contains("Q2")
                          .contains("A2");
    }

    @Test
    void name_returnsHistory() {
        assertThat(tool.name()).isEqualTo("history");
    }
}
