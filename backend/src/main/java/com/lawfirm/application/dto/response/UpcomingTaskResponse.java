package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.Task.TaskPriority;
import com.lawfirm.domain.model.Task.TaskStatus;

import java.time.LocalDate;

public record UpcomingTaskResponse(
    Long id,
    String title,
    LocalDate dueDate,
    TaskStatus status,
    TaskPriority priority,
    Long caseId,
    String caseNumber,
    Long assignedLawyerId,
    String assignedLawyerName
) {}
