package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.CalendarEvent.EventType;

import java.time.LocalDateTime;

public record CalendarEventResponse(
    Long id,
    String title,
    String description,
    EventType eventType,
    LocalDateTime startDatetime,
    LocalDateTime endDatetime,
    boolean allDay,
    Long caseId,
    String caseNumber,
    Long createdByUserId,
    String createdByUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
