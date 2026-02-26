package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CaseTemplateRequest;
import com.lawfirm.application.dto.response.CaseTemplateResponse;
import com.lawfirm.application.service.CaseTemplateService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cases/templates")
@RequiredArgsConstructor
@Tag(name = "Case Templates", description = "Manage case creation templates")
public class CaseTemplateController {

    private final CaseTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CASE_READ')")
    @Operation(summary = "List all case templates")
    public ResponseEntity<List<CaseTemplateResponse>> findAll() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CASE_CREATE')")
    @Operation(summary = "Create a case template")
    public ResponseEntity<CaseTemplateResponse> create(
        @Valid @RequestBody CaseTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_DELETE')")
    @Operation(summary = "Delete a case template")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
