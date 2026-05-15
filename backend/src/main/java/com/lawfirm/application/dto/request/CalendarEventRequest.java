package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.CalendarEvent.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CalendarEventRequest(
    @NotBlank @Size(max = 255) String title,
    String description,
    @NotNull EventType eventType,
    @NotNull LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    boolean allDay,
    Long caseId
) {}
