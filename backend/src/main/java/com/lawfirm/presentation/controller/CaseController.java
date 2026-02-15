package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.ChangeStatusRequest;
import com.lawfirm.application.dto.request.CreateCaseRequest;
import com.lawfirm.application.dto.request.UpdateCaseRequest;
import com.lawfirm.application.dto.response.CaseResponse;
import com.lawfirm.application.dto.response.CaseSummary;
import com.lawfirm.application.service.CaseService;
import com.lawfirm.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Cases", description = "Case/Dossier management")
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CASE_CREATE')")
    @Operation(summary = "Create new case")
    public ResponseEntity<CaseResponse> createCase(
        @Valid @RequestBody CreateCaseRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(caseService.createCase(request, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_READ')")
    @Operation(summary = "Get case by ID")
    public ResponseEntity<CaseResponse> getCaseById(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.findById(id));
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CASE_READ')")
    @Operation(summary = "Search/list cases")
    public ResponseEntity<Page<CaseSummary>> searchCases(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String caseTypeCode,
        @RequestParam(required = false) String categoryCode,
        @RequestParam(required = false) String tribunalCode,
        @RequestParam(required = false) Long lawyerId,
        @RequestParam(required = false) String statusCode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateTo,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "registrationDate") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(caseService.searchCases(
            year, caseTypeCode, categoryCode, tribunalCode, lawyerId, statusCode,
            registrationDateFrom, registrationDateTo, search, pageable
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_UPDATE')")
    @Operation(summary = "Update case")
    public ResponseEntity<CaseResponse> updateCase(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCaseRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(caseService.updateCase(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'CASE_UPDATE')")
    @Operation(summary = "Change case status")
    public ResponseEntity<CaseResponse> changeStatus(
        @PathVariable Long id,
        @Valid @RequestBody ChangeStatusRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(caseService.changeStatus(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_DELETE')")
    @Operation(summary = "Delete case (soft delete)")
    public ResponseEntity<Void> deleteCase(@PathVariable Long id) {
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }
}
