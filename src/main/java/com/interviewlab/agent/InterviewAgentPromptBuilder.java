package com.interviewlab.agent;

import com.interviewlab.session.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Assembles InterviewAgent prompts from session context and AgentTool results.
 * System prompt and instructions from AgentProperties — never hardcoded here.
 */
@RequiredArgsConstructor
@Component
public class InterviewAgentPromptBuilder {

    private final AgentProperties agentProperties;

    public String buildSessionPrompt(Session session, Map<String, String> toolResults) {
        return agentProperties.interviewSystemPrompt() + "\n\n"
            + "Interview Type: " + session.getInterviewType() + "\n"
            + "Target Role: "    + session.getTargetRole()    + "\n"
            + "Job Description:\n" + session.getJdText()      + "\n\n"
            + buildPhaseContext(1)
            + buildToolContext(toolResults)
            + "Ask the first interview question.";
    }

    public String buildFollowUpPrompt(Session session, int nextQuestionNumber, String lastQuestion,
                                      String candidateAnswer, Map<String, String> toolResults) {
        return agentProperties.interviewSystemPrompt() + "\n\n"
            + "Interview Type: "   + session.getInterviewType() + "\n"
            + "Target Role: "      + session.getTargetRole()    + "\n"
            + "Last Question: "    + lastQuestion               + "\n"
            + "Candidate Answer: " + candidateAnswer            + "\n\n"
            + buildPhaseContext(nextQuestionNumber)
            + buildToolContext(toolResults)
            + agentProperties.followUpInstruction();
    }

    public String buildNextQuestionPrompt(Session session, int questionNumber,
                                          Map<String, String> toolResults) {
        return agentProperties.interviewSystemPrompt() + "\n\n"
            + "Interview Type: " + session.getInterviewType() + "\n"
            + "Target Role: "    + session.getTargetRole()    + "\n"
            + "Question number: " + questionNumber + "\n\n"
            + buildPhaseContext(questionNumber)
            + buildToolContext(toolResults)
            + agentProperties.moveOnInstruction();
    }

    private String buildPhaseContext(int questionNumber) {
        InterviewPhase phase = InterviewPhase.forQuestionNumber(questionNumber);
        return "Current phase: " + phase.label() + ". " + phase.instruction() + "\n\n";
    }

    private String buildToolContext(Map<String, String> toolResults) {
        StringBuilder sb = new StringBuilder();
        toolResults.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                sb.append("[").append(key).append("]\n").append(value).append("\n\n");
            }
        });
        return sb.toString();
    }
}
