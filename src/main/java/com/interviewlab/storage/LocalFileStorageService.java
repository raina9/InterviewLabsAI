package com.interviewlab.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public LocalFileStorageService(@Value("${app.storage.local-path:./storage}") String localPath) {
        this.basePath = Path.of(localPath);
        log.info("storage.mode=local basePath={}", this.basePath.toAbsolutePath());
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
}
