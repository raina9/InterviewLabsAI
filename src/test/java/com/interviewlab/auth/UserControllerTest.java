package com.interviewlab.auth;

import com.interviewlab.config.SecurityConfig;
import com.interviewlab.profile.UserProfileService;
import com.interviewlab.storage.StorageException;
import com.interviewlab.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    // Required by SecurityConfig — provide mocks so the context loads
    @MockitoBean JwtService           jwtService;
    @MockitoBean OAuth2SuccessHandler  oauth2SuccessHandler;

    @MockitoBean UserAccountService userAccountService;
    @MockitoBean StorageService     storageService;
    @MockitoBean UserProfileService userProfileService;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken authToken() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "user@example.com", "Test User", null);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void deleteAccount_authenticated_returns204AndDelegatesToService() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(
            USER_ID, "user@example.com", "Test User", null
        );
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, List.of());

        mockMvc.perform(delete("/api/v1/me").with(authentication(auth)))
            .andExpect(status().isNoContent());

        verify(userAccountService).deleteAccount(USER_ID);
    }

    @Test
    void deleteAccount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/me").with(anonymous()))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/me/resume
    // -------------------------------------------------------------------------

    @Test
    void uploadResume_validPdf_returns200AndResumeUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", "%PDF-1.4 fake resume content".getBytes());
        String resumeUrl = "/files/" + USER_ID + "/generated.pdf";
        when(storageService.store(any(MultipartFile.class), eq(USER_ID.toString()))).thenReturn(resumeUrl);

        mockMvc.perform(multipart("/api/v1/me/resume").file(file).with(authentication(authToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumeUrl").value(resumeUrl));

        verify(userProfileService).updateResumeUrl(USER_ID, resumeUrl);
    }

    @Test
    void uploadResume_nonPdf_returns400InvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.txt", "text/plain", "not a pdf".getBytes());
        when(storageService.store(any(MultipartFile.class), eq(USER_ID.toString())))
            .thenThrow(new StorageException(ErrorCode.INVALID_FILE_TYPE, HttpStatus.BAD_REQUEST,
                "Only PDF files are accepted for resume upload."));

        mockMvc.perform(multipart("/api/v1/me/resume").file(file).with(authentication(authToken())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_FILE_TYPE"));
    }

    @Test
    void uploadResume_oversized_returns400FileTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", "%PDF-1.4 oversized".getBytes());
        when(storageService.store(any(MultipartFile.class), eq(USER_ID.toString())))
            .thenThrow(new StorageException(ErrorCode.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST,
                "File size exceeds the maximum allowed 5MB."));

        mockMvc.perform(multipart("/api/v1/me/resume").file(file).with(authentication(authToken())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"));
    }

    @Test
    void uploadResume_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", "%PDF-1.4".getBytes());

        mockMvc.perform(multipart("/api/v1/me/resume").file(file).with(anonymous()))
            .andExpect(status().isUnauthorized());
    }
}
