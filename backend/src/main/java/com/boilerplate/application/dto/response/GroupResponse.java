package com.boilerplate.application.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record GroupResponse(
    Long id,
    String name,
    String description,
    Set<RoleResponse> roles,
    Set<UserSummary> users,  // Optional, populated in detail view
    Integer userCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }
