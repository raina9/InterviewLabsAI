package com.interviewlab.agent;

import com.interviewlab.agent.tools.AgentContext;
import com.interviewlab.agent.tools.AgentTool;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentToolChainTest {

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Test
    void execute_callsAllToolsAndKeysByName() {
        AgentTool tool1 = mock(AgentTool.class);
        AgentTool tool2 = mock(AgentTool.class);
        when(tool1.name()).thenReturn("resume");
        when(tool2.name()).thenReturn("history");
        when(tool1.execute(any())).thenReturn("Resume text");
        when(tool2.execute(any())).thenReturn("INTERVIEWER: question");

        AgentToolChain chain = new AgentToolChain(List.of(tool1, tool2));
        AgentContext ctx     = new AgentContext(USER_ID, SESSION_ID, null, null);

        Map<String, String> results = chain.execute(ctx);

        assertThat(results).containsEntry("resume",  "Resume text")
                           .containsEntry("history", "INTERVIEWER: question");
    }

    @Test
    void execute_callsToolsInOrder() {
        AgentTool tool1 = mock(AgentTool.class);
        AgentTool tool2 = mock(AgentTool.class);
        AgentTool tool3 = mock(AgentTool.class);
        when(tool1.name()).thenReturn("resume");
        when(tool2.name()).thenReturn("history");
        when(tool3.name()).thenReturn("proficiency");
        when(tool1.execute(any())).thenReturn("");
        when(tool2.execute(any())).thenReturn("");
        when(tool3.execute(any())).thenReturn("");

        AgentToolChain chain = new AgentToolChain(List.of(tool1, tool2, tool3));
        AgentContext ctx     = new AgentContext(USER_ID, SESSION_ID, null, null);
        chain.execute(ctx);

        InOrder order = inOrder(tool1, tool2, tool3);
        order.verify(tool1).execute(ctx);
        order.verify(tool2).execute(ctx);
        order.verify(tool3).execute(ctx);
    }

    @Test
    void execute_failingTool_contributesEmptyAndChainContinues() {
        AgentTool failingTool = mock(AgentTool.class);
        AgentTool goodTool    = mock(AgentTool.class);
        when(failingTool.name()).thenReturn("bad");
        when(goodTool.name()).thenReturn("good");
        when(failingTool.execute(any())).thenThrow(new RuntimeException("DB error"));
        when(goodTool.execute(any())).thenReturn("good result");

        AgentToolChain chain = new AgentToolChain(List.of(failingTool, goodTool));
        AgentContext ctx     = new AgentContext(USER_ID, SESSION_ID, null, null);

        Map<String, String> results = chain.execute(ctx);

        assertThat(results).containsEntry("bad", "")
                           .containsEntry("good", "good result");
    }
}
