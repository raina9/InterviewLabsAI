package com.interviewlab.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.auth.AuthenticatedUser;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.auth.JwtService;
import com.interviewlab.auth.OAuth2SuccessHandler;
import com.interviewlab.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CodeChallengeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CodeChallengeControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean CodeChallengeService  codeChallengeService;
    @MockitoBean JwtService            jwtService;
    @MockitoBean OAuth2SuccessHandler  oauth2SuccessHandler;

    private static final UUID USER_ID      = UUID.randomUUID();
    private static final UUID CHALLENGE_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private CodeChallenge sampleChallenge() {
        return new CodeChallenge(
            CHALLENGE_ID, "Two Sum",
            "Find two numbers that add up to a target.",
            Map.of("java", "// Java starter", "python", "# Python starter"),
            List.of("Input: [2,7,11,15] target=9 Expected: [0,1]"),
            List.of("2 <= nums.length <= 10^4")
        );
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/code/challenge
    // -------------------------------------------------------------------------

    @Test
    void challenge_validRequest_returns200WithChallenge() throws Exception {
        when(codeChallengeService.generateChallenge(any(CodeChallengeRequest.class)))
            .thenReturn(sampleChallenge());

        mockMvc.perform(post("/api/v1/code/challenge")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Arrays\",\"difficulty\":\"medium\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(CHALLENGE_ID.toString()))
            .andExpect(jsonPath("$.title").value("Two Sum"))
            .andExpect(jsonPath("$.starterCode").exists())
            .andExpect(jsonPath("$.testCases").isArray());
    }

    @Test
    void challenge_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/code/challenge")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Arrays\",\"difficulty\":\"easy\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void challenge_blankTopic_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/code/challenge")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"\",\"difficulty\":\"easy\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void challenge_blankDifficulty_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/code/challenge")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Strings\",\"difficulty\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/code/submit
    // -------------------------------------------------------------------------

    @Test
    void submit_validSolution_returns200WithReview() throws Exception {
        CodeSubmitResponse response = new CodeSubmitResponse(
            true, "Correct — uses HashMap for O(n) lookup.",
            "// refined", "Two-pass HashMap approach.", "Status: Accepted"
        );
        when(codeChallengeService.submitSolution(any(CodeSubmitRequest.class))).thenReturn(response);

        CodeSubmitRequest request = new CodeSubmitRequest(CHALLENGE_ID, "HashMap<Integer,Integer> map = new HashMap<>();", "java");

        mockMvc.perform(post("/api/v1/code/submit")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passed").value(true))
            .andExpect(jsonPath("$.feedback").isNotEmpty());
    }

    @Test
    void submit_unknownChallenge_returns404() throws Exception {
        when(codeChallengeService.submitSolution(any())).thenThrow(
            new CodeChallengeException(ErrorCode.CODE_CHALLENGE_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Code challenge " + CHALLENGE_ID + " not found.")
        );

        CodeSubmitRequest request = new CodeSubmitRequest(CHALLENGE_ID, "int[] a = {};", "java");

        mockMvc.perform(post("/api/v1/code/submit")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("CODE_CHALLENGE_NOT_FOUND"));
    }

    @Test
    void submit_unauthenticated_returns401() throws Exception {
        CodeSubmitRequest request = new CodeSubmitRequest(CHALLENGE_ID, "// code", "java");

        mockMvc.perform(post("/api/v1/code/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/code/challenge/{id}/hint
    // -------------------------------------------------------------------------

    @Test
    void hint_knownChallenge_returns200WithHint() throws Exception {
        when(codeChallengeService.getHint(CHALLENGE_ID))
            .thenReturn("Think about using a HashMap to store previously seen numbers.");

        mockMvc.perform(get("/api/v1/code/challenge/{id}/hint", CHALLENGE_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("HashMap")));
    }

    @Test
    void hint_unknownChallenge_returns404() throws Exception {
        when(codeChallengeService.getHint(CHALLENGE_ID)).thenThrow(
            new CodeChallengeException(ErrorCode.CODE_CHALLENGE_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Code challenge " + CHALLENGE_ID + " not found.")
        );

        mockMvc.perform(get("/api/v1/code/challenge/{id}/hint", CHALLENGE_ID)
                .with(authentication(authToken())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("CODE_CHALLENGE_NOT_FOUND"));
    }

    @Test
    void hint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/code/challenge/{id}/hint", CHALLENGE_ID))
            .andExpect(status().isUnauthorized());
    }
}
