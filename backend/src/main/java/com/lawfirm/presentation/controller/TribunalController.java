package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.TribunalResponse;
import com.lawfirm.application.service.TribunalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tribunals")
@RequiredArgsConstructor
@Tag(name = "Tribunals", description = "Tribunal reference data management")
public class TribunalController {

    private final TribunalService tribunalService;

    @GetMapping
    @Operation(summary = "Search active tribunals (supports ?search=&page=&size=)")
    public ResponseEntity<Page<TribunalResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            tribunalService.search(search, PageRequest.of(page, size, Sort.by("nameFr")))
        );
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get tribunal by code (public reference data)")
    public ResponseEntity<TribunalResponse> getTribunalByCode(@PathVariable String code) {
        return ResponseEntity.ok(tribunalService.findByCode(code));
    }
}
