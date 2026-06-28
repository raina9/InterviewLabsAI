package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Active Ollama provider — routes to a locally running Ollama instance via Spring AI OllamaChatModel.
 * Zero cost, zero external API calls. Connection config from spring.ai.ollama.* in application.yml.
 * Per-call temperature and token budget come from AIOptions — agent decides, not the provider.
 * Gemini parked (GeminiProvider stays wired; factory routes to it when AiProvider.GEMINI is selected).
 */
@Slf4j
@Component
public final class OllamaProvider implements AiProviderStrategy {

    private final OllamaChatModel chatModel;

    public OllamaProvider(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
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
        return AiProvider.OLLAMA;
    }

    private String call(String prompt, AIOptions options, boolean jsonMode) {
        try {
            OllamaChatOptions.Builder builder = OllamaChatOptions.builder()
                .model(chatModel.getDefaultOptions().getModel())
                .temperature((double) options.temperature())
                .numPredict(options.maxTokens());

            if (jsonMode) {
                builder.format("json");
            }

            log.debug("Calling Ollama at model={}", chatModel.getDefaultOptions().getModel());
            Prompt ollamaPrompt = new Prompt(List.of(new UserMessage(prompt)), builder.build());
            String content = chatModel.call(ollamaPrompt)
                .getResult()
                .getOutput()
                .getText();

            if (content == null || content.isBlank()) {
                throw new AIProviderException(
                    ErrorCode.AI_RESPONSE_PARSE_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ollama returned an empty response — check that the model is loaded and responding"
                );
            }

            log.debug("Ollama call succeeded: jsonMode={} contentLength={}", jsonMode, content.length());
            return content;

        } catch (AIProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Ollama call failed: {}", ex.getMessage());
            throw new AIProviderException(
                ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                "Ollama call failed — ensure Ollama is running at the configured base URL"
            );
        }
    }
}
