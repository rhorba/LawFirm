package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.TimeEntryRequest;
import com.lawfirm.application.dto.response.TimeEntryResponse;
import com.lawfirm.application.dto.response.TimeEntrySummaryResponse;
import com.lawfirm.application.service.TimeEntryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Time Tracking", description = "Time entry logging and billing summaries")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    // ── Per-case ──────────────────────────────────────────────────────────────

    @GetMapping("/api/cases/{caseId}/time-entries")
    @PreAuthorize("hasPermission(null, 'TIME_READ')")
    @Operation(summary = "List time entries for a case")
    public ResponseEntity<List<TimeEntryResponse>> listByCaseId(@PathVariable Long caseId) {
        return ResponseEntity.ok(timeEntryService.findByCaseId(caseId));
    }

    @GetMapping("/api/cases/{caseId}/time-entries/summary")
    @PreAuthorize("hasPermission(null, 'TIME_READ')")
    @Operation(summary = "Get time & billing summary for a case")
    public ResponseEntity<TimeEntrySummaryResponse> summary(@PathVariable Long caseId) {
        return ResponseEntity.ok(timeEntryService.getSummary(caseId));
    }

    @PostMapping("/api/cases/{caseId}/time-entries")
    @PreAuthorize("hasPermission(null, 'TIME_CREATE')")
    @Operation(summary = "Log a time entry on a case")
    public ResponseEntity<TimeEntryResponse> create(
            @PathVariable Long caseId,
            @Valid @RequestBody TimeEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeEntryService.create(caseId, request));
    }

    // ── Individual entry operations ───────────────────────────────────────────

    @PutMapping("/api/time-entries/{id}")
    @PreAuthorize("hasPermission(null, 'TIME_UPDATE')")
    @Operation(summary = "Update a time entry")
    public ResponseEntity<TimeEntryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TimeEntryRequest request) {
        return ResponseEntity.ok(timeEntryService.update(id, request));
    }

    @DeleteMapping("/api/time-entries/{id}")
    @PreAuthorize("hasPermission(null, 'TIME_DELETE')")
    @Operation(summary = "Delete a time entry")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timeEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/time-entries/{id}/bill")
    @PreAuthorize("hasPermission(null, 'TIME_MANAGE')")
    @Operation(summary = "Mark a time entry as billed, optionally linking to an invoice")
    public ResponseEntity<TimeEntryResponse> markAsBilled(
            @PathVariable Long id,
            @RequestParam(required = false) Long invoiceId) {
        return ResponseEntity.ok(timeEntryService.markAsBilled(id, invoiceId));
    }
}
