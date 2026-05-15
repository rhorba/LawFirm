package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CalendarEventRequest;
import com.lawfirm.application.dto.response.CalendarDayEvent;
import com.lawfirm.application.dto.response.CalendarEventResponse;
import com.lawfirm.application.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "Calendar events and scheduling")
public class CalendarController {

    private final CalendarService calendarService;

    // ── Month view (calendar events + task deadlines merged) ──────────────────

    @GetMapping("/month")
    @PreAuthorize("hasPermission(null, 'CALENDAR_READ')")
    @Operation(summary = "Get all events for a month (calendar events + task deadlines)")
    public ResponseEntity<List<CalendarDayEvent>> getMonth(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate ref = LocalDate.now();
        int y = year != null ? year : ref.getYear();
        int m = month != null ? month : ref.getMonthValue();
        return ResponseEntity.ok(calendarService.getMonthEvents(y, m));
    }

    // ── Per-case events ───────────────────────────────────────────────────────

    @GetMapping("/cases/{caseId}")
    @PreAuthorize("hasPermission(null, 'CALENDAR_READ')")
    @Operation(summary = "List calendar events for a case")
    public ResponseEntity<List<CalendarEventResponse>> listByCaseId(@PathVariable Long caseId) {
        return ResponseEntity.ok(calendarService.findByCaseId(caseId));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CALENDAR_CREATE')")
    @Operation(summary = "Create a calendar event")
    public ResponseEntity<CalendarEventResponse> create(
            @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CALENDAR_READ')")
    @Operation(summary = "Get a calendar event by id")
    public ResponseEntity<CalendarEventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(calendarService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CALENDAR_UPDATE')")
    @Operation(summary = "Update a calendar event")
    public ResponseEntity<CalendarEventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(calendarService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CALENDAR_DELETE')")
    @Operation(summary = "Delete a calendar event")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendarService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
