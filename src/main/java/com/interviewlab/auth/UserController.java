package com.interviewlab.auth;

import com.interviewlab.profile.UserProfileService;
import com.interviewlab.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Account", description = "Authenticated user's own account management")
public class UserController {

    private final UserAccountService userAccountService;
    private final StorageService     storageService;
    private final UserProfileService userProfileService;

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload resume PDF",
        description = "Validates (content-type + magic bytes + size), stores the PDF via the configured " +
            "storage backend (local filesystem or S3), and updates the authenticated user's profile " +
            "with the resulting resume URL.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Resume uploaded and profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid file type or file too large"),
            @ApiResponse(responseCode = "401", description = "No valid authentication present")
        }
    )
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("file") MultipartFile file) {
        if (principal == null) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_MISSING);
        }
        String resumeUrl = storageService.store(file, principal.id().toString());
        userProfileService.updateResumeUrl(principal.id(), resumeUrl);
        log.info("Resume uploaded: userId={}", principal.id());
        return ResponseEntity.ok(new ResumeUploadResponse(resumeUrl));
    }

    @DeleteMapping
    @Operation(
        summary = "Permanently delete the authenticated user's account and all associated data",
        description = "GDPR-style right to erasure. Deletes answer feedback, messages, proficiency, " +
            "sessions, profile, and the user record itself. Irreversible.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Account and all data deleted"),
            @ApiResponse(responseCode = "401", description = "No valid authentication present")
        }
    )
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new AuthException(ErrorCode.AUTH_TOKEN_MISSING);
        }
        userAccountService.deleteAccount(principal.id());
        log.info("Account deletion requested via API: userId={}", principal.id());
        return ResponseEntity.noContent().build();
    }
}
