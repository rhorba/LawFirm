package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.Task.TaskPriority;
import com.lawfirm.domain.model.Task.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    String title,
    String description,
    LocalDate dueDate,
    TaskStatus status,
    TaskPriority priority,
    Long caseId,
    String caseNumber,
    Long assignedLawyerId,
    String assignedLawyerName,
    Long createdByUserId,
    String createdByUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
