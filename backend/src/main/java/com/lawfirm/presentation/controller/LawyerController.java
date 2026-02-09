package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CreateLawyerRequest;
import com.lawfirm.application.dto.request.UpdateLawyerRequest;
import com.lawfirm.application.dto.response.LawyerResponse;
import com.lawfirm.application.service.LawyerService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lawyers")
@RequiredArgsConstructor
@Tag(name = "Lawyers", description = "Lawyer management")
public class LawyerController {

    private final LawyerService lawyerService;

    @GetMapping
    @Operation(summary = "Get all active lawyers (public reference data)")
    public ResponseEntity<List<LawyerResponse>> getAllLawyers() {
        return ResponseEntity.ok(lawyerService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lawyer by ID (public reference data)")
    public ResponseEntity<LawyerResponse> getLawyerById(@PathVariable Long id) {
        return ResponseEntity.ok(lawyerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'LAWYER_CREATE')")
    @Operation(summary = "Create new lawyer")
    public ResponseEntity<LawyerResponse> createLawyer(@Valid @RequestBody CreateLawyerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lawyerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'LAWYER_UPDATE')")
    @Operation(summary = "Update lawyer")
    public ResponseEntity<LawyerResponse> updateLawyer(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLawyerRequest request
    ) {
        return ResponseEntity.ok(lawyerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'LAWYER_DELETE')")
    @Operation(summary = "Deactivate lawyer")
    public ResponseEntity<Void> deactivateLawyer(@PathVariable Long id) {
        lawyerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/cases/count")
    @PreAuthorize("hasPermission(null, 'LAWYER_READ')")
    @Operation(summary = "Get active case count for lawyer")
    public ResponseEntity<Long> getLawyerCaseCount(@PathVariable Long id) {
        return ResponseEntity.ok(lawyerService.getCaseCount(id));
    }
}
