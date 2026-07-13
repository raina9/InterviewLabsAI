package com.interviewlab.storage;

import com.interviewlab.auth.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage — active by default (STORAGE_MODE=local or unset).
 *
 * Swap to S3StorageService by setting STORAGE_MODE=s3. See pom.xml for AWS SDK activation
 * and storage/package-info.java for the full migration path.
 *
 * AWS naming: the class that replaces this is S3StorageService — naming convention preserved.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements StorageService {

    private final Path basePath;
    private final Path uploadBasePath;
    private final StorageProperties storageProperties;

    public LocalFileStorageService(
            @Value("${app.storage.local-path:./storage}") String localPath,
            StorageProperties storageProperties) {
        this.basePath = Path.of(localPath);
        this.storageProperties = storageProperties;
        this.uploadBasePath = Path.of(storageProperties.uploadDir());
        log.info("storage.mode=local basePath={} uploadBasePath={}",
            this.basePath.toAbsolutePath(), this.uploadBasePath.toAbsolutePath());
    }

    @Override
    public void store(String key, byte[] data) {
        Path target = basePath.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            log.debug("storage.store key={} bytes={}", key, data.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store object at key: " + key, e);
        }
    }

    @Override
    public byte[] retrieve(String key) {
        Path target = basePath.resolve(key);
        try {
            byte[] data = Files.readAllBytes(target);
            log.debug("storage.retrieve key={} bytes={}", key, data.length);
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to retrieve object at key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        Path target = basePath.resolve(key);
        try {
            boolean deleted = Files.deleteIfExists(target);
            log.debug("storage.delete key={} deleted={}", key, deleted);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete object at key: " + key, e);
        }
    }

    @Override
    public String store(MultipartFile file, String userId) {
        ResumeFileValidator.validate(file, storageProperties);

        String fileKey = userId + "/" + UUID.randomUUID() + ".pdf";
        Path target = uploadBasePath.resolve(fileKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException(ErrorCode.STORAGE_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to store resume for user " + userId);
        }
        log.info("storage.resume.store userId={} fileKey={}", userId, fileKey);
        return getUrl(fileKey);
    }

    @Override
    public String getUrl(String fileKey) {
        return "/files/" + fileKey;
    }
}
