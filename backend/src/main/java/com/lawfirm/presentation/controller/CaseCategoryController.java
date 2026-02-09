package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.CaseCategoryResponse;
import com.lawfirm.application.mapper.CaseCategoryMapper;
import com.lawfirm.domain.repository.CaseCategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/case-categories")
@RequiredArgsConstructor
@Tag(name = "Case Categories", description = "Case category reference data")
public class CaseCategoryController {

    private final CaseCategoryRepository caseCategoryRepository;
    private final CaseCategoryMapper caseCategoryMapper;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all case categories (public reference data)")
    public ResponseEntity<List<CaseCategoryResponse>> getAllCaseCategories() {
        List<CaseCategoryResponse> categories = caseCategoryRepository.findAll()
                .stream()
                .map(caseCategoryMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }
}
