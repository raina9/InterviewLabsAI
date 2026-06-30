package com.interviewlab.quiz;

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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class QuizControllerTest {

    @Autowired MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean QuizService          quizService;
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;

    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "test@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private QuizSession sampleSession() {
        return new QuizSession(
            SESSION_ID, "Java", "medium", 5, 0, 0,
            "What is the time complexity of HashMap.get()?",
            List.of("O(1)", "O(n)", "O(log n)", "O(n log n)")
        );
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/quiz/start
    // -------------------------------------------------------------------------

    @Test
    void start_validRequest_returns200WithFirstQuestion() throws Exception {
        when(quizService.startQuiz(any(StartQuizRequest.class))).thenReturn(sampleSession());

        StartQuizRequest request = new StartQuizRequest("Java", "medium", 5);

        mockMvc.perform(post("/api/v1/quiz/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
            .andExpect(jsonPath("$.topic").value("Java"))
            .andExpect(jsonPath("$.currentQuestion").isNotEmpty())
            .andExpect(jsonPath("$.currentOptions").isArray());
    }

    @Test
    void start_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/quiz/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Java\",\"difficulty\":\"medium\",\"questionCount\":5}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void start_blankTopic_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/quiz/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"\",\"difficulty\":\"medium\",\"questionCount\":5}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void start_questionCountBelowMin_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/quiz/start")
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"Java\",\"difficulty\":\"easy\",\"questionCount\":1}"))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/quiz/{sessionId}/answer
    // -------------------------------------------------------------------------

    @Test
    void answer_correctAnswer_returns200WithFeedback() throws Exception {
        QuizAnswerResponse response = new QuizAnswerResponse(
            true, "Correct explanation.", "O(1)", 1, 1, false,
            "Which data structure uses LIFO?", List.of("Queue", "Stack", "Tree", "Heap")
        );
        when(quizService.submitAnswer(eq(SESSION_ID), eq("O(1)"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/quiz/{sessionId}/answer", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"O(1)\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.correct").value(true))
            .andExpect(jsonPath("$.score").value(1))
            .andExpect(jsonPath("$.sessionComplete").value(false));
    }

    @Test
    void answer_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/quiz/{sessionId}/answer", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"O(1)\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void answer_blankAnswer_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/quiz/{sessionId}/answer", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void answer_unknownSession_returns404() throws Exception {
        when(quizService.submitAnswer(eq(SESSION_ID), any())).thenThrow(
            new QuizException(ErrorCode.QUIZ_SESSION_NOT_FOUND, HttpStatus.NOT_FOUND,
                "Quiz session " + SESSION_ID + " not found.")
        );

        mockMvc.perform(post("/api/v1/quiz/{sessionId}/answer", SESSION_ID)
                .with(authentication(authToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\":\"O(1)\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("QUIZ_SESSION_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/quiz/{sessionId}/result
    // -------------------------------------------------------------------------

    @Test
    void result_completedSession_returns200WithScore() throws Exception {
        QuizResult result = new QuizResult(5, 4, 80, java.util.Map.of("Java", 4));
        when(quizService.getResult(SESSION_ID)).thenReturn(result);

        mockMvc.perform(get("/api/v1/quiz/{sessionId}/result", SESSION_ID)
                .with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalQuestions").value(5))
            .andExpect(jsonPath("$.correctAnswers").value(4))
            .andExpect(jsonPath("$.scorePercent").value(80));
    }

    @Test
    void result_notYetComplete_returns409() throws Exception {
        when(quizService.getResult(SESSION_ID)).thenThrow(
            new QuizException(ErrorCode.QUIZ_NOT_YET_COMPLETE, HttpStatus.CONFLICT,
                "Quiz session " + SESSION_ID + " is not yet complete.")
        );

        mockMvc.perform(get("/api/v1/quiz/{sessionId}/result", SESSION_ID)
                .with(authentication(authToken())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("QUIZ_NOT_YET_COMPLETE"));
    }

    @Test
    void result_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/quiz/{sessionId}/result", SESSION_ID))
            .andExpect(status().isUnauthorized());
    }
}
