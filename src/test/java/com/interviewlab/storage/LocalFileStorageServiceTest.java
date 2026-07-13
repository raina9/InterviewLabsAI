package com.interviewlab.storage;

import com.interviewlab.auth.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir Path tempDir;

    private LocalFileStorageService storageService;

    private static final String USER_ID = "user-123";

    // StorageProperties is a @ConfigurationProperties record — Mockito can't supply it via
    // @InjectMocks, so it's constructed for real (same pattern as AiProperties/DrillProperties
    // in TopicDrillServiceTest). uploadDir points at the JUnit temp dir so no file is ever
    // written outside the test sandbox.
    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties(
            "local", tempDir.toString(), 5, List.of("application/pdf"), "", "us-east-1"
        );
        storageService = new LocalFileStorageService(tempDir.toString(), storageProperties);
    }

    @Test
    void store_validPdf_returnsUrlAndPersistsFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", "%PDF-1.4 valid resume content".getBytes());

        String url = storageService.store(file, USER_ID);

        assertThat(url).startsWith("/files/" + USER_ID + "/").endsWith(".pdf");

        String fileKey = url.substring("/files/".length());
        assertThat(Files.exists(tempDir.resolve(fileKey))).isTrue();
    }

    @Test
    void store_wrongMagicBytesDespitePdfExtension_throwsInvalidFileType() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", "NOT A REAL PDF FILE".getBytes());

        assertThatThrownBy(() -> storageService.store(file, USER_ID))
            .isInstanceOf(StorageException.class)
            .satisfies(ex -> {
                StorageException se = (StorageException) ex;
                assertThat(se.errorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE);
                assertThat(se.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            });
    }

    @Test
    void store_oversizedFile_throwsFileTooLarge() {
        byte[] oversized = new byte[6 * 1024 * 1024]; // 6MB > 5MB configured limit
        oversized[0] = '%';
        oversized[1] = 'P';
        oversized[2] = 'D';
        oversized[3] = 'F';
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> storageService.store(file, USER_ID))
            .isInstanceOf(StorageException.class)
            .satisfies(ex -> assertThat(((StorageException) ex).errorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }
}
