package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.response.TribunalResponse;
import com.boilerplate.application.service.TribunalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tribunals")
@RequiredArgsConstructor
@Tag(name = "Tribunals", description = "Tribunal reference data management")
public class TribunalController {

    private final TribunalService tribunalService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all active tribunals")
    public ResponseEntity<List<TribunalResponse>> getAllTribunals() {
        return ResponseEntity.ok(tribunalService.findAll());
    }

    @GetMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get tribunal by code")
    public ResponseEntity<TribunalResponse> getTribunalByCode(@PathVariable String code) {
        return ResponseEntity.ok(tribunalService.findByCode(code));
    }
}
