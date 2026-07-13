package com.interviewlab.storage;

import com.interviewlab.auth.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.UUID;

/**
 * S3 object storage — active when app.storage.mode=s3 (STORAGE_MODE=s3 env var).
 * Free/OSS default equivalent: LocalFileStorageService. See pom.xml and package-info.java
 * for the AWS SDK activation steps and required env vars (AWS_S3_BUCKET/AWS_REGION/credentials).
 *
 * AWS naming: S3StorageService / S3Client — consistent with aws-mapping.md.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public S3StorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        Region region = Region.of(storageProperties.s3Region());
        this.s3Client = S3Client.builder().region(region).build();
        this.s3Presigner = S3Presigner.builder().region(region).build();
        log.info("storage.mode=s3 bucket={} region={}", storageProperties.s3Bucket(), storageProperties.s3Region());
    }

    @Override
    public void store(String key, byte[] data) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(storageProperties.s3Bucket()).key(key).build(),
            RequestBody.fromBytes(data)
        );
        log.debug("s3.store key={} bytes={}", key, data.length);
    }

    @Override
    public byte[] retrieve(String key) {
        try (InputStream in = s3Client.getObject(
                GetObjectRequest.builder().bucket(storageProperties.s3Bucket()).key(key).build())) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to retrieve object at key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(storageProperties.s3Bucket()).key(key).build());
        log.debug("s3.delete key={}", key);
    }

    @Override
    public String store(MultipartFile file, String userId) {
        ResumeFileValidator.validate(file, storageProperties);

        String fileKey = "resumes/" + userId + "/" + UUID.randomUUID() + ".pdf";
        try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(storageProperties.s3Bucket())
                    .key(fileKey)
                    .contentType("application/pdf")
                    .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new StorageException(ErrorCode.STORAGE_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to upload resume for user " + userId);
        }
        log.info("s3.resume.store userId={} fileKey={}", userId, fileKey);
        return getUrl(fileKey);
    }

    @Override
    public String getUrl(String fileKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(1))
            .getObjectRequest(GetObjectRequest.builder().bucket(storageProperties.s3Bucket()).key(fileKey).build())
            .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
