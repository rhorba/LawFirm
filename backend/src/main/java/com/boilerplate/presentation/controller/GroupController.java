package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.request.GroupAssignUsersRequest;
import com.boilerplate.application.dto.request.GroupRequest;
import com.boilerplate.application.dto.response.GroupResponse;
import com.boilerplate.application.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Groups", description = "Group management endpoints")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Get all groups", description = "Retrieve list of all groups with roles and user counts")
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        List<GroupResponse> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Get group by ID", description = "Retrieve a specific group by its ID")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id) {
        GroupResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Create new group", description = "Create a new group with optional role assignments")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        GroupResponse group = groupService.createGroup(request);
        return new ResponseEntity<>(group, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Update group", description = "Update an existing group's details and role assignments")
    public ResponseEntity<GroupResponse> updateGroup(
        @PathVariable Long id,
        @Valid @RequestBody GroupRequest request
    ) {
        GroupResponse group = groupService.updateGroup(id, request);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Delete group", description = "Delete a group (only if it has no users)")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/users")
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Assign users to group", description = "Add multiple users to a group")
    public ResponseEntity<GroupResponse> assignUsers(
        @PathVariable Long id,
        @Valid @RequestBody GroupAssignUsersRequest request
    ) {
        GroupResponse group = groupService.assignUsersToGroup(id, request);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}/users/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_MANAGE')")
    @Operation(summary = "Remove user from group", description = "Remove a specific user from a group")
    public ResponseEntity<Void> removeUser(
        @PathVariable Long groupId,
        @PathVariable Long userId
    ) {
        groupService.removeUserFromGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }
}
