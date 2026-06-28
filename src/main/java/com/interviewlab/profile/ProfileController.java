package com.interviewlab.profile;

import com.interviewlab.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "User profile — candidate context for AI personalisation")
public class ProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "Get current user profile",
               description = "Returns or creates the profile for the authenticated user.")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ProfileResponse.from(userProfileService.getOrCreateProfile(principal.id())));
    }

    @PutMapping
    @Operation(summary = "Update profile metadata",
               description = "Updates experience years, current role, tech stack, and preferred AI provider. All fields are optional.")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(ProfileResponse.from(userProfileService.updateProfile(principal.id(), request)));
    }

    @PutMapping("/resume")
    @Operation(summary = "Update resume text",
               description = "Replaces the stored ATS-format resume text used for interview context.")
    public ResponseEntity<ProfileResponse> updateResume(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid UpdateResumeRequest request) {
        return ResponseEntity.ok(ProfileResponse.from(userProfileService.updateResumeText(principal.id(), request.resumeText())));
    }

    @PutMapping("/custom-prompt")
    @Operation(summary = "Update custom prompt",
               description = "Saves a custom instruction applied automatically to every subsequent session.")
    public ResponseEntity<ProfileResponse> updateCustomPrompt(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody @Valid UpdateCustomPromptRequest request) {
        return ResponseEntity.ok(ProfileResponse.from(userProfileService.updateCustomPrompt(principal.id(), request.customPrompt())));
    }
}
