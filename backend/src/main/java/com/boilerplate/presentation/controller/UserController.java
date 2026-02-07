package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.BulkActionRequest;
import com.boilerplate.application.dto.request.BulkStatusRequest;
import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.request.UserSearchRequest;
import com.boilerplate.application.dto.response.BulkActionResponse;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Search users", description = "Search and filter users with pagination")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false, defaultValue = "false") Boolean showDeleted,
        Pageable pageable
    ) {
        UserSearchRequest searchRequest = UserSearchRequest.builder()
            .search(search)
            .role(role)
            .enabled(enabled)
            .showDeleted(showDeleted)
            .build();
        return ResponseEntity.ok(userService.searchUsers(searchRequest, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by ID", description = "Retrieve user details by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get user by username", description = "Retrieve user details by username")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Update user", description = "Update existing user")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Delete user", description = "Delete user by ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Restore user", description = "Restore a soft-deleted user")
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.restoreUser(id));
    }

    @DeleteMapping("/{id}/purge")
    @PreAuthorize("hasAuthority('USER_DELETE') and hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Purge user", description = "Permanently delete a soft-deleted user")
    public ResponseEntity<Void> purgeUser(@PathVariable Long id) {
        userService.purgeUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Operation(summary = "Bulk delete users", description = "Soft-delete multiple users")
    public ResponseEntity<BulkActionResponse> bulkDelete(@Valid @RequestBody BulkActionRequest request) {
        int affected = userService.bulkSoftDelete(request.getUserIds());
        return ResponseEntity.ok(BulkActionResponse.builder()
            .affected(affected)
            .message(affected + " users deleted")
            .build());
    }

    @PostMapping("/bulk/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Bulk update status", description = "Enable or disable multiple users")
    public ResponseEntity<BulkActionResponse> bulkUpdateStatus(@Valid @RequestBody BulkStatusRequest request) {
        int affected = userService.bulkUpdateStatus(request.getUserIds(), request.getEnabled());
        return ResponseEntity.ok(BulkActionResponse.builder()
            .affected(affected)
            .message(affected + " users updated")
            .build());
    }
}
