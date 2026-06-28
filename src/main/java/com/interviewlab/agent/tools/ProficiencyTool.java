package com.interviewlab.agent.tools;

import com.interviewlab.proficiency.ProficiencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Order(3)
public class ProficiencyTool implements AgentTool {

    private final ProficiencyRepository proficiencyRepository;

    @Override
    public String name() {
        return "proficiency";
    }

    @Override
    public String execute(AgentContext context) {
        return proficiencyRepository.findByUserId(context.userId())
            .stream()
            .map(p -> p.getTopic() + ":" + p.getScore())
            .collect(Collectors.joining("\n"));
    }
}
