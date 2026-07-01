package com.interviewlab.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiProviderTest {

    RestClient.Builder    restClientBuilder;
    MockRestServiceServer mockServer;
    GeminiProvider        geminiProvider;

    AiProperties aiProperties = new AiProperties(
        AiProvider.GEMINI,
        120,
        new AiProperties.GeminiConfig(
            "gemini-test-model",
            "http://mock-gemini.test/v1beta/models",
            "test-api-key"
        ),
        new AiProperties.OptionsConfig(0.7f, 800, 0.3f, 500, 0.7f, 1000),
        new AiProperties.QuizOptions(0.7f, 1000),
        new AiProperties.CodeOptions(0.7f, 1000, 0.3f, 800),
        new AiProperties.CurriculumOptions(0.5f, 1000),
        new AiProperties.DrillOptions(0.7f, 800, 0.3f, 500, 0.5f, 700)
    );

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer        = MockRestServiceServer.bindTo(restClientBuilder).build();
        geminiProvider    = new GeminiProvider(aiProperties, restClientBuilder, new ObjectMapper());
    }

    @Test
    void generate_successfulResponse_returnsText() {
        String responseJson = """
            {
              "candidates": [{
                "content": {
                  "parts": [{"text": "Hello, I can help with that."}]
                }
              }]
            }
            """;

        mockServer.expect(requestTo(containsString("generateContent")))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = geminiProvider.generate("Tell me something", AIOptions.defaults());

        assertThat(result).isEqualTo("Hello, I can help with that.");
        mockServer.verify();
    }

    @Test
    void generate_401Response_throwsAuthTokenInvalid() {
        mockServer.expect(requestTo(containsString("generateContent")))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> geminiProvider.generate("test", AIOptions.defaults()))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> assertThat(((AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID));
    }

    @Test
    void generate_429Response_throwsRateLimitExceeded() {
        mockServer.expect(requestTo(containsString("generateContent")))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> geminiProvider.generate("test", AIOptions.defaults()))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> assertThat(((AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    void generate_5xxResponse_throwsAIServiceUnavailable() {
        mockServer.expect(requestTo(containsString("generateContent")))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> geminiProvider.generate("test", AIOptions.defaults()))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> assertThat(((AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE));
    }

    @Test
    void generateJson_successfulResponse_returnsText() {
        String responseJson = """
            {
              "candidates": [{
                "content": {
                  "parts": [{"text": "{\\"score\\": 8}"}]
                }
              }]
            }
            """;

        mockServer.expect(requestTo(containsString("generateContent")))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String result = geminiProvider.generateJson("Return score as JSON", AIOptions.defaults());

        assertThat(result).contains("score");
    }

    @Test
    void generate_malformedResponse_throwsParseFailure() {
        mockServer.expect(requestTo(containsString("generateContent")))
            .andRespond(withSuccess("{\"unexpected\": \"structure\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiProvider.generate("test", AIOptions.defaults()))
            .isInstanceOf(AIProviderException.class)
            .satisfies(ex -> assertThat(((AIProviderException) ex).errorCode())
                .isEqualTo(ErrorCode.AI_RESPONSE_PARSE_FAILED));
    }

    @Test
    void providerName_returnsGemini() {
        assertThat(geminiProvider.providerName()).isEqualTo(AiProvider.GEMINI);
    }
}
