package com.boilerplate.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resource;
    private String resourceId;
    private String metadata;
    private String ipAddress;
    private LocalDateTime createdAt;
}
