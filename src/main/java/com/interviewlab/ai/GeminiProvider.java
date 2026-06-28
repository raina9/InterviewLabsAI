package com.interviewlab.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.auth.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Active Gemini V1 provider — calls Gemini generateContent REST API via RestClient.
 * All config (model, api-url, api-key) from AiProperties — never hardcoded.
 * Token budgets and temperature from AIOptions passed per call — agent decides, not the provider.
 */
@Slf4j
@Component
public final class GeminiProvider implements AiProviderStrategy {

    private final AiProperties aiProperties;
    private final RestClient   restClient;
    private final ObjectMapper objectMapper;

    public GeminiProvider(AiProperties aiProperties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.restClient   = restClientBuilder
            .baseUrl(aiProperties.gemini().apiUrl())
            .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String generate(String prompt, AIOptions options) {
        return call(prompt, options, false);
    }

    @Override
    public String generateJson(String prompt, AIOptions options) {
        return call(prompt, options, true);
    }

    @Override
    public AiProvider providerName() {
        return AiProvider.GEMINI;
    }

    private String call(String prompt, AIOptions options, boolean jsonMode) {
        try {
            String requestBody = buildRequest(prompt, options, jsonMode);
            String model       = aiProperties.gemini().model();
            String apiKey      = aiProperties.gemini().apiKey();

            String responseBody = restClient.post()
                .uri(ub -> ub.path("/" + model + ":generateContent")
                             .queryParam("key", apiKey)
                             .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 401, (req, resp) -> {
                    throw new AIProviderException(
                        ErrorCode.AUTH_TOKEN_INVALID, HttpStatus.UNAUTHORIZED,
                        "Gemini API authentication failed — verify GEMINI_API_KEY"
                    );
                })
                .onStatus(status -> status.value() == 429, (req, resp) -> {
                    throw new AIProviderException(
                        ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS,
                        "Gemini API rate limit exceeded — retry after a pause"
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new AIProviderException(
                        ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini service is currently unavailable — please retry"
                    );
                })
                .body(String.class);

            log.debug("Gemini call succeeded: model={} jsonMode={}", model, jsonMode);
            return extractText(responseBody);

        } catch (AIProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Gemini API call failed: {}", ex.getMessage());
            throw new AIProviderException(
                ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini API call failed — please try again"
            );
        }
    }

    private String buildRequest(String prompt, AIOptions options, boolean jsonMode) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature",     options.temperature());
        generationConfig.put("maxOutputTokens", options.maxTokens());
        if (jsonMode) {
            generationConfig.put("responseMimeType", "application/json");
        }

        Map<String, Object> requestMap = Map.of(
            "contents",        List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", generationConfig
        );

        try {
            return objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException ex) {
            throw new AIProviderException(
                ErrorCode.AI_RESPONSE_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to serialize Gemini API request"
            );
        }
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root     = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text");

            if (textNode == null || textNode.isMissingNode() || textNode.isNull()) {
                throw new AIProviderException(
                    ErrorCode.AI_RESPONSE_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gemini response missing expected text content in candidates[0].content.parts[0].text"
                );
            }
            return textNode.asText();
        } catch (AIProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response: {}", ex.getMessage());
            throw new AIProviderException(
                ErrorCode.AI_RESPONSE_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to parse Gemini API response"
            );
        }
    }
}
