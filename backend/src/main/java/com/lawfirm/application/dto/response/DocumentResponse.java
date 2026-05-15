package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.DocumentCategory;

import java.time.LocalDateTime;

public record DocumentResponse(
    Long id,
    Long caseId,
    String caseNumber,
    String title,
    String description,
    DocumentCategory category,
    String originalFilename,
    String contentType,
    Long fileSize,
    String uploadedByUsername,
    LocalDateTime createdAt
) {}
