package com.lawfirm.application.dto.response;

import java.time.LocalDate;

/**
 * Unified event for the calendar grid — covers both CalendarEvents and Task due dates.
 * Source: "CALENDAR_EVENT" | "TASK"
 */
public record CalendarDayEvent(
    Long id,
    String title,
    String type,
    String source,
    LocalDate date,
    Long caseId,
    String caseNumber
) {}
