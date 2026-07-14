package com.interviewlab.agent;

import com.interviewlab.agent.tools.AgentContext;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.ai.AiProvider;
import com.interviewlab.ai.AiProviderStrategy;
import com.interviewlab.session.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewAgentTest {

    @Mock AgentToolChain             agentToolChain;
    @Mock InterviewAgentPromptBuilder promptBuilder;
    @Mock AIProviderFactory          aiProviderFactory;
    @Mock SessionRepository          sessionRepository;
    @Mock MessageService             messageService;
    @Mock AiProviderStrategy         aiProvider;

    InterviewAgent interviewAgent;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private final AiProperties aiProperties = new AiProperties(
        AiProvider.OLLAMA,
        120,
        new AiProperties.GeminiConfig("gemini-flash-lite-latest", "http://test", "key"),
        new AiProperties.OptionsConfig(0.7f, 800, 0.3f, 500, 0.7f, 1000),
        new AiProperties.QuizOptions(0.7f, 1000),
        new AiProperties.CodeOptions(0.7f, 1000, 0.3f, 800),
        new AiProperties.CurriculumOptions(0.5f, 1000),
        new AiProperties.DrillOptions(0.7f, 800, 0.3f, 500, 0.5f, 700)
    );

    private static final int TOTAL_QUESTIONS = 3;

    private final AgentProperties agentProperties = new AgentProperties(
        5, TOTAL_QUESTIONS, "You are an interviewer.", "You are a mentor.",
        "Ask a follow-up.", "Move to the next question."
    );

    @BeforeEach
    void setUp() {
        interviewAgent = new InterviewAgent(
            agentToolChain, promptBuilder, aiProviderFactory, sessionRepository, messageService,
            aiProperties, agentProperties
        );
        when(aiProviderFactory.getDefaultProvider()).thenReturn(aiProvider);
    }

    @Test
    void initSession_returnsFirstQuestion() {
        Session session = new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(agentToolChain.execute(any(AgentContext.class))).thenReturn(Map.of("resume", ""));
        when(promptBuilder.buildSessionPrompt(any(), any())).thenReturn("Interview prompt");
        when(aiProvider.generate(eq("Interview prompt"), any())).thenReturn("Tell me about your Java experience.");
        when(messageService.addMessage(any(), any(), any(), anyBoolean()))
            .thenReturn(new Message(SESSION_ID, MessageRole.INTERVIEWER, "Tell me about your Java experience.", 1, false));

        String result = interviewAgent.initSession(USER_ID, SESSION_ID);

        assertThat(result).isEqualTo("Tell me about your Java experience.");
        verify(messageService).addMessage(SESSION_ID, MessageRole.INTERVIEWER, "Tell me about your Java experience.", false);
    }

    @Test
    void initSession_sessionNotFound_throwsSessionException() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> interviewAgent.initSession(USER_ID, SESSION_ID))
            .isInstanceOf(SessionException.class);
    }

    @Test
    void nextTurn_savesAnswerAndReturnsFollowUp() {
        Session session = new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        // 1 of TOTAL_QUESTIONS (3) already asked — turn is not the last one.
        Message prevQuestion = new Message(SESSION_ID, MessageRole.INTERVIEWER, "Tell me about Java.", 1, false);
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(List.of(prevQuestion));
        when(agentToolChain.execute(any(AgentContext.class))).thenReturn(Map.of());
        when(promptBuilder.buildFollowUpPrompt(any(), anyInt(), any(), any(), any())).thenReturn("Follow-up prompt");
        when(aiProvider.generate(eq("Follow-up prompt"), any())).thenReturn("Can you describe a complex problem you solved?");

        UUID candidateMsgId = UUID.randomUUID();
        Message candidateMsg = mock(Message.class);
        when(candidateMsg.getId()).thenReturn(candidateMsgId);
        when(messageService.addMessage(eq(SESSION_ID), eq(MessageRole.CANDIDATE), any(), anyBoolean()))
            .thenReturn(candidateMsg);
        when(messageService.addMessage(eq(SESSION_ID), eq(MessageRole.INTERVIEWER), any(), anyBoolean()))
            .thenReturn(new Message(SESSION_ID, MessageRole.INTERVIEWER, "Can you describe a complex problem you solved?", 3, false));

        InterviewTurnResult result = interviewAgent.nextTurn(USER_ID, SESSION_ID, "My answer about Java", false);

        assertThat(result.agentResponse()).isEqualTo("Can you describe a complex problem you solved?");
        assertThat(result.shouldMoveToNextQuestion()).isFalse();
        assertThat(result.currentQuestionNumber()).isEqualTo(1);
        assertThat(result.candidateMessageId()).isEqualTo(candidateMsgId);

        verify(messageService).addMessage(SESSION_ID, MessageRole.CANDIDATE, "My answer about Java", false);
        verify(messageService).addMessage(SESSION_ID, MessageRole.INTERVIEWER, "Can you describe a complex problem you solved?", false);
    }

    @Test
    void nextTurn_finalQuestionOfTotal_sessionCompletes() {
        Session session = new Session(USER_ID, InterviewType.TECHNICAL, "Senior Engineer", "JD", "MEDIUM", SessionStatus.ACTIVE);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        // TOTAL_QUESTIONS (3) already asked — candidate is answering the last one.
        List<Message> history = List.of(
            new Message(SESSION_ID, MessageRole.INTERVIEWER, "Q1", 1, false),
            new Message(SESSION_ID, MessageRole.CANDIDATE, "A1", 2, false),
            new Message(SESSION_ID, MessageRole.INTERVIEWER, "Q2", 3, false),
            new Message(SESSION_ID, MessageRole.CANDIDATE, "A2", 4, false),
            new Message(SESSION_ID, MessageRole.INTERVIEWER, "Q3", 5, false)
        );
        when(messageService.getSessionMessages(SESSION_ID)).thenReturn(history);
        when(agentToolChain.execute(any(AgentContext.class))).thenReturn(Map.of());
        when(promptBuilder.buildFollowUpPrompt(any(), anyInt(), any(), any(), any())).thenReturn("Follow-up prompt");
        when(aiProvider.generate(eq("Follow-up prompt"), any())).thenReturn("Closing remarks.");

        UUID candidateMsgId = UUID.randomUUID();
        Message candidateMsg = mock(Message.class);
        when(candidateMsg.getId()).thenReturn(candidateMsgId);
        when(messageService.addMessage(eq(SESSION_ID), eq(MessageRole.CANDIDATE), any(), anyBoolean()))
            .thenReturn(candidateMsg);
        when(messageService.addMessage(eq(SESSION_ID), eq(MessageRole.INTERVIEWER), any(), anyBoolean()))
            .thenReturn(new Message(SESSION_ID, MessageRole.INTERVIEWER, "Closing remarks.", 6, false));

        InterviewTurnResult result = interviewAgent.nextTurn(USER_ID, SESSION_ID, "My answer to Q3", false);

        assertThat(result.currentQuestionNumber()).isEqualTo(TOTAL_QUESTIONS);
        assertThat(result.shouldMoveToNextQuestion()).isTrue();
    }
}
