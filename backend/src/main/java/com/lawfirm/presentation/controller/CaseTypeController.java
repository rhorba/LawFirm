package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.CaseTypeResponse;
import com.lawfirm.application.mapper.CaseTypeMapper;
import com.lawfirm.domain.repository.CaseTypeRepository;
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
@RequestMapping("/api/case-types")
@RequiredArgsConstructor
@Tag(name = "Case Types", description = "Case type reference data")
public class CaseTypeController {

    private final CaseTypeRepository caseTypeRepository;
    private final CaseTypeMapper caseTypeMapper;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all case types (public reference data)")
    public ResponseEntity<List<CaseTypeResponse>> getAllCaseTypes() {
        List<CaseTypeResponse> caseTypes = caseTypeRepository.findAll()
                .stream()
                .map(caseTypeMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(caseTypes);
    }
}
