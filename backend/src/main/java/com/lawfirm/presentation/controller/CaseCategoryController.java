package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.CaseCategoryResponse;
import com.lawfirm.application.mapper.CaseCategoryMapper;
import com.lawfirm.domain.model.CaseCategory;
import com.lawfirm.domain.repository.CaseCategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/case-categories")
@RequiredArgsConstructor
@Tag(name = "Case Categories", description = "Case category reference data")
public class CaseCategoryController {

    private final CaseCategoryRepository caseCategoryRepository;
    private final CaseCategoryMapper caseCategoryMapper;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Search case categories (supports ?search=&caseTypeCode=&page=&size=)")
    public ResponseEntity<Page<CaseCategoryResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String caseTypeCode,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<CaseCategory> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("active"), true));

            if (caseTypeCode != null && !caseTypeCode.isBlank()) {
                predicates.add(cb.equal(root.get("caseType").get("code"), caseTypeCode));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nameFr")), pattern),
                    cb.like(cb.lower(root.get("nameAr")), pattern),
                    cb.like(cb.lower(root.get("code")),   pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CaseCategoryResponse> result = caseCategoryRepository
            .findAll(spec, PageRequest.of(page, size, Sort.by("nameFr")))
            .map(caseCategoryMapper::toResponse);

        return ResponseEntity.ok(result);
    }
}
