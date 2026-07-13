package com.interviewlab.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final int requestTimeoutSeconds;
    private final String storageMode;
    private final String uploadDir;

    public WebMvcConfig(
            @Value("${app.ai.request-timeout-seconds:120}") int requestTimeoutSeconds,
            @Value("${app.storage.mode:local}") String storageMode,
            @Value("${app.storage.upload-dir:uploads/}") String uploadDir) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.storageMode = storageMode;
        this.uploadDir = uploadDir;
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        // Timeout covers Ollama cold start (model load takes 30-40s) + AI response generation.
        // Override via AI_REQUEST_TIMEOUT_SECONDS env var.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(requestTimeoutSeconds));
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Serve uploaded resume PDFs directly from disk — only meaningful in local storage
        // mode; S3 mode returns presigned URLs pointing at the bucket instead (see
        // S3StorageService.getUrl), so this handler is never hit when app.storage.mode=s3.
        if ("local".equalsIgnoreCase(storageMode)) {
            String location = Path.of(uploadDir).toUri().toString();
            registry.addResourceHandler("/files/**")
                    .addResourceLocations(location);
        }
    }
}
