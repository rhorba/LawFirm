package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.PermissionResponse;
import com.lawfirm.application.mapper.PermissionMapper;
import com.lawfirm.domain.repository.PermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Permissions", description = "Permission management endpoints")
public class PermissionController {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @Operation(summary = "Get all permissions", description = "Retrieve list of all permissions")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> permissions = permissionRepository.findAll().stream()
            .map(permissionMapper::toResponse)
            .toList();
        return ResponseEntity.ok(permissions);
    }
}
