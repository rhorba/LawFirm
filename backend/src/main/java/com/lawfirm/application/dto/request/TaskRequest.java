package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.Task.TaskPriority;
import com.lawfirm.domain.model.Task.TaskStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotNull @Future LocalDate dueDate,
    @NotNull TaskStatus status,
    @NotNull TaskPriority priority,
    Long assignedLawyerId
) {}
