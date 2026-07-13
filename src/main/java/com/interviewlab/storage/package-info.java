/**
 * Object storage abstraction — Swappable Backend Pattern.
 *
 * Active (default): LocalFileStorageService
 *   Condition: @ConditionalOnProperty(name="app.storage.mode", havingValue="local", matchIfMissing=true)
 *   Config:    STORAGE_MODE=local (or unset), STORAGE_LOCAL_PATH=./storage
 *   Cost:      zero — local filesystem
 *
 * Paid (cloud): S3StorageService (built — see storage/S3StorageService.java)
 *   Condition: @ConditionalOnProperty(name="app.storage.mode", havingValue="s3")
 *   Config:    STORAGE_MODE=s3, AWS_S3_BUCKET, AWS_REGION, AWS credentials
 *   Activation: set STORAGE_MODE=s3 and AWS_S3_BUCKET/AWS_REGION/credentials in environment —
 *     software.amazon.awssdk:s3 is already on the classpath (pom.xml), zero code change required.
 *
 * AWS naming convention: S3StorageService (matches GlobalBrain naming pattern).
 * MinIO is the local/OSS drop-in replacement for S3 — same SDK, different endpoint.
 */
package com.interviewlab.storage;
