package com.interviewlab.agent.tools;

import com.interviewlab.ai.AiProvider;
import com.interviewlab.profile.UserProfile;
import com.interviewlab.profile.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeContextToolTest {

    @Mock UserProfileRepository userProfileRepository;
    @InjectMocks ResumeContextTool tool;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private AgentContext ctx() {
        return new AgentContext(USER_ID, SESSION_ID, null, null);
    }

    @Test
    void execute_profileWithResume_returnsResumeText() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        profile.setResumeText("10 years Java experience at FAANG");
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        String result = tool.execute(ctx());

        assertThat(result).isEqualTo("10 years Java experience at FAANG");
    }

    @Test
    void execute_profileWithNoResume_returnsEmpty() {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        String result = tool.execute(ctx());

        assertThat(result).isEmpty();
    }

    @Test
    void execute_noProfile_returnsEmpty() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        String result = tool.execute(ctx());

        assertThat(result).isEmpty();
    }

    @Test
    void name_returnsResume() {
        assertThat(tool.name()).isEqualTo("resume");
    }
}
