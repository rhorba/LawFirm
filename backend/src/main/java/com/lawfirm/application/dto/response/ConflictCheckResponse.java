package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.ConflictCheck.CheckResult;

import java.time.LocalDateTime;

public record ConflictCheckResponse(
    Long id,
    String searchName,
    CheckResult result,
    String clearedNote,
    String performedByUsername,
    String clearedByUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
