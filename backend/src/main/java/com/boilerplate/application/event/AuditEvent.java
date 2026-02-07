package com.boilerplate.application.event;

public record AuditEvent(
    Long userId,
    String username,
    String action,
    String resource,
    String resourceId,
    String metadata,
    String ipAddress
) {}
