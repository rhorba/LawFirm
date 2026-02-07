package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.UpdateProfileRequest;
import com.boilerplate.application.dto.response.UserProfileResponse;
import com.boilerplate.application.service.UserProfileService;
import com.boilerplate.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for managing extended user profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the currently authenticated user. Returns 404 if profile does not exist.")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userProfileService.getProfile(currentUser.getUser().getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Upsert current user profile", description = "Creates or updates the profile of the currently authenticated user.")
    public ResponseEntity<UserProfileResponse> upsertMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.upsertProfile(currentUser.getUser().getId(), request));
    }
}
