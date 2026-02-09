package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.CaseStatusResponse;
import com.lawfirm.application.mapper.CaseStatusMapper;
import com.lawfirm.domain.repository.CaseStatusRepository;
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
@RequestMapping("/api/case-statuses")
@RequiredArgsConstructor
@Tag(name = "Case Statuses", description = "Case status reference data")
public class CaseStatusController {

    private final CaseStatusRepository caseStatusRepository;
    private final CaseStatusMapper caseStatusMapper;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all case statuses (public reference data)")
    public ResponseEntity<List<CaseStatusResponse>> getAllCaseStatuses() {
        List<CaseStatusResponse> statuses = caseStatusRepository.findAll()
                .stream()
                .map(caseStatusMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }
}
