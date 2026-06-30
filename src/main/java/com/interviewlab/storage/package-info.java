/**
 * Object storage abstraction — Swappable Backend Pattern.
 *
 * Active (default): LocalFileStorageService
 *   Condition: @ConditionalOnProperty(name="app.storage.mode", havingValue="local", matchIfMissing=true)
 *   Config:    STORAGE_MODE=local (or unset), STORAGE_LOCAL_PATH=./storage
 *   Cost:      zero — local filesystem
 *
 * Paid (cloud): S3StorageService (not yet built — seam is ready)
 *   Condition: @ConditionalOnProperty(name="app.storage.mode", havingValue="s3")
 *   Config:    STORAGE_MODE=s3, AWS_S3_BUCKET, AWS_REGION, AWS credentials
 *   Activation steps:
 *     1. Uncomment software.amazon.awssdk:s3 in pom.xml
 *     2. Uncomment aws.sdk.version in pom.xml properties
 *     3. Uncomment AWS SDK BOM in dependencyManagement
 *     4. Create S3StorageService implementing StorageService (AWS naming convention: S3StorageService)
 *     5. Annotate with @Service @ConditionalOnProperty(name="app.storage.mode", havingValue="s3")
 *     6. Set STORAGE_MODE=s3 and AWS_ credentials in environment
 *
 * AWS naming convention: S3StorageService (matches GlobalBrain naming pattern).
 * MinIO is the local/OSS drop-in replacement for S3 — same SDK, different endpoint.
 */
package com.interviewlab.storage;
