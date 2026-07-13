package com.interviewlab.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound from app.storage.* in application.yml.
 * Registered via @ConfigurationPropertiesScan on InterviewLabApplication.
 * uploadDir/maxFileSizeMb/allowedTypes govern resume upload (store(MultipartFile,userId));
 * mode/s3Bucket/s3Region select and configure the active StorageService implementation.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
    String       mode,
    String       uploadDir,
    long         maxFileSizeMb,
    List<String> allowedTypes,
    String       s3Bucket,
    String       s3Region
) {}
