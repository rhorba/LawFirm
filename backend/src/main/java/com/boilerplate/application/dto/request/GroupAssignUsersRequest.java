package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record GroupAssignUsersRequest(
    @NotEmpty(message = "User IDs cannot be empty")
    Set<Long> userIds
) {}
