package com.lawfirm.application.event;

public record AuditEvent(
    Long userId,
    String username,
    String action,
    String resource,
    String resourceId,
    String metadata,
    String ipAddress
) {}
