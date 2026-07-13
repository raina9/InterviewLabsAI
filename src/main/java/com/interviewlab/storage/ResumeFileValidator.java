package com.interviewlab.storage;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Shared resume-upload validation for every StorageService implementation
 * (LocalFileStorageService, S3StorageService) — content-type AND magic-byte
 * check, since a renamed non-PDF file still passes a content-type check alone.
 */
final class ResumeFileValidator {

    private static final byte[] PDF_MAGIC_BYTES = {'%', 'P', 'D', 'F'};

    private ResumeFileValidator() {}

    static void validate(MultipartFile file, StorageProperties properties) {
        if (file == null || file.isEmpty()) {
            throw new StorageException(ErrorCode.INVALID_FILE_TYPE, HttpStatus.BAD_REQUEST,
                "Uploaded file is empty.");
        }

        long maxBytes = properties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new StorageException(ErrorCode.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST,
                "File size exceeds the maximum allowed " + properties.maxFileSizeMb() + "MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedTypes().contains(contentType)) {
            throw new StorageException(ErrorCode.INVALID_FILE_TYPE, HttpStatus.BAD_REQUEST,
                "Only PDF files are accepted for resume upload.");
        }

        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(PDF_MAGIC_BYTES.length);
        } catch (IOException e) {
            throw new StorageException(ErrorCode.STORAGE_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to read uploaded file.");
        }
        if (header.length < PDF_MAGIC_BYTES.length || !Arrays.equals(header, PDF_MAGIC_BYTES)) {
            throw new StorageException(ErrorCode.INVALID_FILE_TYPE, HttpStatus.BAD_REQUEST,
                "File content does not match a valid PDF.");
        }
    }
}
