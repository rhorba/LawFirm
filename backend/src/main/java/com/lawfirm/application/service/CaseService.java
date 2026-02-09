package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.ChangeStatusRequest;
import com.lawfirm.application.dto.request.CreateCaseRequest;
import com.lawfirm.application.dto.request.UpdateCaseRequest;
import com.lawfirm.application.dto.response.CaseResponse;
import com.lawfirm.application.dto.response.CaseSummary;
import com.lawfirm.application.mapper.CaseMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.CaseCategory;
import com.lawfirm.domain.model.CaseStatus;
import com.lawfirm.domain.model.CaseType;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.model.Tribunal;
import com.lawfirm.domain.repository.CaseCategoryRepository;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.CaseSpecification;
import com.lawfirm.domain.repository.CaseStatusRepository;
import com.lawfirm.domain.repository.CaseTypeRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TribunalRepository;
import com.lawfirm.infrastructure.security.UserPrincipal;
import com.lawfirm.presentation.exception.InvalidStatusTransitionException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseService {

    private final CaseRepository caseRepository;
    private final TribunalRepository tribunalRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final CaseCategoryRepository caseCategoryRepository;
    private final LawyerRepository lawyerRepository;
    private final CaseStatusRepository caseStatusRepository;
    private final CaseSequenceService caseSequenceService;
    private final CaseNumberGenerator caseNumberGenerator;
    private final CaseMapper caseMapper;

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, UserPrincipal currentUser) {
        // 1. Validate references
        CaseType caseType = caseTypeRepository.findByCodeAndActiveTrue(request.caseTypeCode())
            .orElseThrow(() -> new ResourceNotFoundException("CaseType not found with code: " + request.caseTypeCode()));

        Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal not found with code: " + request.tribunalCode()));

        Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(request.lawyerId())
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with id: " + request.lawyerId()));

        // 2. Handle optional case category
        CaseCategory caseCategory = null;
        if (request.caseCategoryCode() != null && !request.caseCategoryCode().isBlank()) {
            caseCategory = caseCategoryRepository.findByCodeAndActiveTrue(request.caseCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("CaseCategory not found with code: " + request.caseCategoryCode()));

            // Validate category belongs to case type
            if (caseCategory.getCaseType() != null &&
                !caseCategory.getCaseType().getCode().equals(caseType.getCode())) {
                throw new IllegalArgumentException(
                    "Case category " + request.caseCategoryCode() +
                    " does not belong to case type " + request.caseTypeCode()
                );
            }
        }

        // 3. Determine initial status
        CaseStatus initialStatus = determineInitialStatus(caseType, request.initialStatusCode());

        // 4. Generate case number
        int year = Year.now().getValue();
        int sequenceNumber = caseSequenceService.getNextSequence(year, caseType.getCode());
        String fullCaseNumber = caseNumberGenerator.generate(
            caseType.getNumberFormatTemplate(),
            year,
            tribunal.getCode(),
            caseType.getCode(),
            sequenceNumber
        );

        // 5. Build and save case
        Case caseEntity = Case.builder()
            .year(year)
            .sequenceNumber(sequenceNumber)
            .fullCaseNumber(fullCaseNumber)
            .registrationDate(request.registrationDate())
            .caseDescription(request.caseDescription())
            .matterDescription(request.matterDescription())
            .tribunal(tribunal)
            .caseType(caseType)
            .caseCategory(caseCategory)
            .lawyer(lawyer)
            .status(initialStatus)
            .build();

        caseEntity = caseRepository.save(caseEntity);

        // 6. TODO: Publish audit event (will be added in later task)

        return caseMapper.toResponse(caseEntity);
    }

    private CaseStatus determineInitialStatus(CaseType caseType, String statusCode) {
        if (statusCode != null && !statusCode.isBlank()) {
            CaseStatus status = caseStatusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStatus not found with code: " + statusCode));

            // Validate status is allowed for this case type
            if (!caseType.getAllowedStatuses().contains(status)) {
                throw new InvalidStatusTransitionException(
                    "Status " + statusCode + " is not allowed for case type " + caseType.getCode()
                );
            }
            return status;
        }

        // Default to first status (DRAFT)
        return caseStatusRepository.findByCode("DRAFT")
            .orElseThrow(() -> new ResourceNotFoundException("CaseStatus not found with code: DRAFT"));
    }

    public CaseResponse findById(Long id) {
        return caseRepository.findByIdWithDetails(id)
            .map(caseMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));
    }

    public Page<CaseSummary> searchCases(
        Integer year,
        String caseTypeCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        java.time.LocalDate registrationDateFrom,
        java.time.LocalDate registrationDateTo,
        Pageable pageable
    ) {
        Specification<Case> spec = CaseSpecification.withFilters(
            year, caseTypeCode, tribunalCode, lawyerId, statusCode,
            registrationDateFrom, registrationDateTo
        );

        return caseRepository.findAll(spec, pageable)
            .map(caseMapper::toSummary);
    }

    @Transactional
    public CaseResponse updateCase(Long id, UpdateCaseRequest request, UserPrincipal currentUser) {
        Case caseEntity = caseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        // Update mutable fields only
        if (request.tribunalCode() != null) {
            Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
                .orElseThrow(() -> new ResourceNotFoundException("Tribunal not found with code: " + request.tribunalCode()));
            caseEntity.setTribunal(tribunal);
        }

        if (request.caseCategoryCode() != null) {
            CaseCategory category = caseCategoryRepository.findByCodeAndActiveTrue(request.caseCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("CaseCategory not found with code: " + request.caseCategoryCode()));
            caseEntity.setCaseCategory(category);
        }

        if (request.lawyerId() != null) {
            Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(request.lawyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with id: " + request.lawyerId()));
            caseEntity.setLawyer(lawyer);
        }

        if (request.registrationDate() != null) {
            caseEntity.setRegistrationDate(request.registrationDate());
        }

        if (request.caseDescription() != null) {
            caseEntity.setCaseDescription(request.caseDescription());
        }

        if (request.matterDescription() != null) {
            caseEntity.setMatterDescription(request.matterDescription());
        }

        caseEntity = caseRepository.save(caseEntity);
        return caseMapper.toResponse(caseEntity);
    }

    @Transactional
    public CaseResponse changeStatus(Long id, ChangeStatusRequest request, UserPrincipal currentUser) {
        Case caseEntity = caseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        CaseStatus newStatus = caseStatusRepository.findByCode(request.statusCode())
            .orElseThrow(() -> new ResourceNotFoundException("CaseStatus not found with code: " + request.statusCode()));

        // Validate status is allowed for this case type
        if (!caseEntity.getCaseType().getAllowedStatuses().contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Status " + newStatus.getCode() + " is not allowed for case type " +
                caseEntity.getCaseType().getCode()
            );
        }

        caseEntity.setStatus(newStatus);
        caseEntity = caseRepository.save(caseEntity);

        // TODO: Publish status changed event

        return caseMapper.toResponse(caseEntity);
    }

    @Transactional
    public void deleteCase(Long id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        // Soft delete
        caseEntity.setDeletedAt(LocalDateTime.now());
        caseRepository.save(caseEntity);
    }
}
