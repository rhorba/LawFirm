package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.response.RoleResponse;
import com.boilerplate.application.mapper.RoleMapper;
import com.boilerplate.domain.repository.RoleRepository;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleController {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @Operation(summary = "Get all roles", description = "Retrieve list of all roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleRepository.findAll().stream()
            .map(roleMapper::toResponse)
            .toList();
        return ResponseEntity.ok(roles);
    }
}
