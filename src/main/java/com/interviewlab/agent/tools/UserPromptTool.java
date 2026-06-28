package com.interviewlab.agent.tools;

import com.interviewlab.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Order(5)
public class UserPromptTool implements AgentTool {

    private final UserProfileRepository userProfileRepository;

    @Override
    public String name() {
        return "userPrompt";
    }

    @Override
    public String execute(AgentContext context) {
        return userProfileRepository.findByUserId(context.userId())
            .map(profile -> profile.getCustomPrompt())
            .filter(cp -> cp != null && !cp.isBlank())
            .orElse("");
    }
}
