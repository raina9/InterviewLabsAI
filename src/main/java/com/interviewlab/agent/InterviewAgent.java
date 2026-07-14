package com.interviewlab.agent;

import com.interviewlab.agent.tools.AgentContext;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.ai.AiProperties;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.session.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interview flow agent — question generation and session turn management ONLY.
 * Does NOT evaluate answers or generate feedback (MentorAgent responsibility).
 * shouldMoveToNextQuestion=true once the candidate has answered agentProperties
 * .totalQuestions() questions — InterviewService.respond() reads this to trigger
 * session auto-completion (see docs/forgekit/AppFlow.md interview-loop flow).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class InterviewAgent {

    private final AgentToolChain             agentToolChain;
    private final InterviewAgentPromptBuilder promptBuilder;
    private final AIProviderFactory          aiProviderFactory;
    private final SessionRepository          sessionRepository;
    private final MessageService             messageService;
    private final AiProperties               aiProperties;
    private final AgentProperties            agentProperties;

    public String initSession(UUID userId, UUID sessionId) {
        Session session = findSessionOrThrow(sessionId);

        AgentContext ctx       = new AgentContext(userId, sessionId, null, null);
        Map<String, String> toolResults = agentToolChain.execute(ctx);

        String prompt   = promptBuilder.buildSessionPrompt(session, toolResults);
        String question = aiProviderFactory.getDefaultProvider().generate(prompt, questionsOptions());

        messageService.addMessage(sessionId, MessageRole.INTERVIEWER, question, false);
        log.info("InterviewAgent initiated: sessionId={} userId={}", sessionId, userId);
        return question;
    }

    public InterviewTurnResult nextTurn(UUID userId, UUID sessionId, String candidateAnswer, boolean voiceUsed) {
        Session      session  = findSessionOrThrow(sessionId);
        List<Message> history = messageService.getSessionMessages(sessionId);

        int questionNumber = (int) history.stream()
            .filter(m -> m.getRole() == MessageRole.INTERVIEWER)
            .count();

        String lastQuestion = history.stream()
            .filter(m -> m.getRole() == MessageRole.INTERVIEWER)
            .reduce((a, b) -> b)
            .map(Message::getContent)
            .orElse("");

        Message candidateMessage = messageService.addMessage(sessionId, MessageRole.CANDIDATE, candidateAnswer, voiceUsed);

        AgentContext ctx       = new AgentContext(userId, sessionId, candidateMessage.getId(), candidateAnswer);
        Map<String, String> toolResults = agentToolChain.execute(ctx);

        String prompt        = promptBuilder.buildFollowUpPrompt(
            session, questionNumber + 1, lastQuestion, candidateAnswer, toolResults);
        String agentResponse = aiProviderFactory.getDefaultProvider().generate(prompt, questionsOptions());

        messageService.addMessage(sessionId, MessageRole.INTERVIEWER, agentResponse, false);

        boolean sessionComplete = questionNumber >= agentProperties.totalQuestions();
        log.debug("InterviewAgent turn: sessionId={} questionNumber={} sessionComplete={}",
            sessionId, questionNumber, sessionComplete);
        return new InterviewTurnResult(agentResponse, sessionComplete, questionNumber, candidateMessage.getId());
    }

    private AIOptions questionsOptions() {
        AiProperties.OptionsConfig opts = aiProperties.options();
        return new AIOptions(opts.questionsTemperature(), opts.questionsMaxTokens(), false);
    }

    private Session findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionException(
                ErrorCode.SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Session " + sessionId + " not found"
            ));
    }
}
