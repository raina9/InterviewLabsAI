package com.interviewlab.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(4)
public class QnAReferenceTool implements AgentTool {

    @Override
    public String name() {
        return "qna";
    }

    @Override
    public String execute(AgentContext context) {
        log.debug("QnAReferenceTool: stub — returning empty (web source integration parked)");
        return "";
    }
}
