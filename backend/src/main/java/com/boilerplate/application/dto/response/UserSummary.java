package com.boilerplate.application.dto.response;

public record UserSummary(
    Long id,
    String username,
    String email,
    Boolean enabled
) { }
