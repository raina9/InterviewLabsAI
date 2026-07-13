package com.interviewlab.storage;

import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Validate (content-type + magic bytes + size) and persist a resume PDF upload for the
     * given user, returning the URL to retrieve it (relative /files/... URL in local mode,
     * presigned URL in S3 mode).
     *
     * @throws StorageException FILE_TOO_LARGE / INVALID_FILE_TYPE / STORAGE_FAILURE
     */
    String store(MultipartFile file, String userId) throws StorageException;

    /**
     * Resolve a previously stored file key to a URL a client can fetch directly.
     */
    String getUrl(String fileKey);
}
