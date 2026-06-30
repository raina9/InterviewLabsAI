package com.interviewlab.storage;

/**
 * Storage abstraction — store, retrieve, and delete keyed binary objects.
 *
 * Free implementation: LocalFileStorageService (local filesystem, zero cost, zero infra).
 * Paid implementation: S3StorageService (aws-sdk/s3 + S3 bucket) — see pom.xml and package-info.java.
 *
 * Switch via STORAGE_MODE env var — no code change required.
 */
public interface StorageService {

    /**
     * Persist binary data under the given key.
     * Key format: "<scope>/<id>/<filename>" e.g. "resumes/user-123/resume.pdf"
     */
    void store(String key, byte[] data);

    /**
     * Retrieve previously stored data. Throws UncheckedIOException if the key does not exist.
     */
    byte[] retrieve(String key);

    /**
     * Delete stored data. No-op if the key does not exist.
     */
    void delete(String key);
}
