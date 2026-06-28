package com.interviewlab.agent.tools;

import com.interviewlab.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Order(1)
public class ResumeContextTool implements AgentTool {

    private final UserProfileRepository userProfileRepository;

    @Override
    public String name() {
        return "resume";
    }

    @Override
    public String execute(AgentContext context) {
        return userProfileRepository.findByUserId(context.userId())
            .map(profile -> profile.getResumeText())
            .filter(rt -> rt != null && !rt.isBlank())
            .orElse("");
    }
}
