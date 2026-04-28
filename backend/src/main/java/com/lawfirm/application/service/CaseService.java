package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.ChangeStatusRequest;
import com.lawfirm.application.dto.request.CreateCaseRequest;
import com.lawfirm.application.dto.request.UpdateCaseRequest;
import com.lawfirm.application.dto.response.CaseResponse;
import com.lawfirm.application.dto.response.CaseSummary;
import com.lawfirm.application.dto.response.CaseSummaryResponse;
import com.lawfirm.application.mapper.CaseMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.CaseCategory;
import com.lawfirm.domain.model.CasePriority;
import com.lawfirm.domain.model.CaseStatus;
import com.lawfirm.domain.model.CaseType;
import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.Lawyer;
import com.lawfirm.domain.model.Tribunal;
import com.lawfirm.domain.repository.CaseCategoryRepository;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.domain.repository.CaseSpecification;
import com.lawfirm.domain.repository.CaseStatusRepository;
import com.lawfirm.domain.repository.CaseTypeRepository;
import com.lawfirm.domain.repository.LawyerRepository;
import com.lawfirm.domain.repository.TribunalRepository;
import com.lawfirm.infrastructure.security.UserPrincipal;
import com.lawfirm.presentation.exception.BusinessRuleException;
import com.lawfirm.presentation.exception.InvalidStatusTransitionException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ClientRepository clientRepository;
    private final CaseSequenceService caseSequenceService;
    private final CaseNumberGenerator caseNumberGenerator;
    private final CaseMapper caseMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request, UserPrincipal currentUser) {
        CaseType caseType = caseTypeRepository.findByCodeAndActiveTrue(request.caseTypeCode())
            .orElseThrow(() -> new ResourceNotFoundException("CaseType not found with code: " + request.caseTypeCode()));

        Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal not found with code: " + request.tribunalCode()));

        Set<Lawyer> lawyers = new HashSet<>(lawyerRepository.findAllById(request.lawyerIds()));
        if (lawyers.size() != request.lawyerIds().size()) {
            throw new ResourceNotFoundException("One or more lawyers not found");
        }

        CaseCategory caseCategory = null;
        if (request.caseCategoryCode() != null && !request.caseCategoryCode().isBlank()) {
            caseCategory = caseCategoryRepository.findByCodeAndActiveTrue(request.caseCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("CaseCategory not found with code: " + request.caseCategoryCode()));

            if (caseCategory.getCaseType() != null &&
                !caseCategory.getCaseType().getCode().equals(caseType.getCode())) {
                throw new IllegalArgumentException(
                    "Case category " + request.caseCategoryCode() +
                    " does not belong to case type " + request.caseTypeCode()
                );
            }
        }

        CaseStatus initialStatus = determineInitialStatus(caseType, request.initialStatusCode());

        int year = Year.now().getValue();
        int sequenceNumber = caseSequenceService.getNextSequence(year, caseType.getCode());
        String fullCaseNumber = caseNumberGenerator.generate(
            caseType.getNumberFormatTemplate(),
            year,
            tribunal.getCode(),
            caseType.getCode(),
            sequenceNumber
        );

        Case parentCase = null;
        if (request.parentCaseId() != null) {
            parentCase = caseRepository.findById(request.parentCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent case not found with id: " + request.parentCaseId()));
        }

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
            .lawyers(lawyers)
            .status(initialStatus)
            .priority(request.priority() != null ? request.priority() : CasePriority.NORMAL)
            .opposingParty(request.opposingParty())
            .outcome(request.outcome())
            .outcomeNotes(request.outcomeNotes())
            .initialPaymentDate(request.initialPaymentDate())
            .fiscalYear(request.fiscalYear())
            .parentCase(parentCase)
            .build();

        if (request.clientId() != null) {
            Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
            caseEntity.setClient(client);
        }

        caseEntity = caseRepository.save(caseEntity);

        auditLogService.log("CASE", caseEntity.getId(), "CASE_CREATED",
            List.of("fullCaseNumber", "priority", "lawyers", "status"), getCurrentUsername());

        return caseMapper.toResponse(caseEntity);
    }

    private CaseStatus determineInitialStatus(CaseType caseType, String statusCode) {
        if (statusCode != null && !statusCode.isBlank()) {
            CaseStatus status = caseStatusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ResourceNotFoundException("CaseStatus not found with code: " + statusCode));

            if (!caseType.getAllowedStatuses().contains(status)) {
                throw new InvalidStatusTransitionException(
                    "Status " + statusCode + " is not allowed for case type " + caseType.getCode()
                );
            }
            return status;
        }

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
        String categoryCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        String priority,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo,
        String search,
        Pageable pageable
    ) {
        Specification<Case> spec = CaseSpecification.withFilters(
            year, caseTypeCode, categoryCode, tribunalCode, lawyerId, statusCode, priority,
            registrationDateFrom, registrationDateTo, search
        );

        return caseRepository.findAll(spec, pageable)
            .map(caseMapper::toSummary);
    }

    public List<Case> findAllByCriteria(
        Integer year,
        String caseTypeCode,
        String categoryCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo,
        String search
    ) {
        Specification<Case> spec = CaseSpecification.withFilters(
            year, caseTypeCode, categoryCode, tribunalCode, lawyerId, statusCode, null,
            registrationDateFrom, registrationDateTo, search
        );
        return caseRepository.findAll(spec);
    }

    public List<CaseSummaryResponse> findChildren(Long parentCaseId) {
        return caseRepository.findByParentCaseIdAndDeletedAtIsNull(parentCaseId)
            .stream()
            .map(c -> new CaseSummaryResponse(c.getId(), c.getFullCaseNumber()))
            .toList();
    }

    @Transactional
    public CaseResponse updateCase(Long id, UpdateCaseRequest request, UserPrincipal currentUser) {
        Case caseEntity = caseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        CaseResponse beforeSnapshot = caseMapper.toResponse(caseEntity);
        List<String> changedFields = new ArrayList<>();

        if (request.tribunalCode() != null) {
            Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
                .orElseThrow(() -> new ResourceNotFoundException("Tribunal not found with code: " + request.tribunalCode()));
            if (!tribunal.equals(caseEntity.getTribunal())) changedFields.add("tribunal");
            caseEntity.setTribunal(tribunal);
        }

        if (request.caseCategoryCode() != null) {
            CaseCategory category = caseCategoryRepository.findByCodeAndActiveTrue(request.caseCategoryCode())
                .orElseThrow(() -> new ResourceNotFoundException("CaseCategory not found with code: " + request.caseCategoryCode()));
            if (!category.equals(caseEntity.getCaseCategory())) changedFields.add("caseCategory");
            caseEntity.setCaseCategory(category);
        }

        if (request.lawyerIds() != null && !request.lawyerIds().isEmpty()) {
            Set<Lawyer> lawyers = new HashSet<>(lawyerRepository.findAllById(request.lawyerIds()));
            if (lawyers.size() != request.lawyerIds().size()) {
                throw new ResourceNotFoundException("One or more lawyers not found");
            }
            Set<Long> existingIds = caseEntity.getLawyers().stream().map(Lawyer::getId).collect(Collectors.toSet());
            if (!existingIds.equals(request.lawyerIds())) changedFields.add("lawyers");
            caseEntity.setLawyers(lawyers);
        }

        if (request.registrationDate() != null && !request.registrationDate().equals(caseEntity.getRegistrationDate())) {
            changedFields.add("registrationDate");
            caseEntity.setRegistrationDate(request.registrationDate());
        }

        if (request.caseDescription() != null && !request.caseDescription().equals(caseEntity.getCaseDescription())) {
            changedFields.add("caseDescription");
            caseEntity.setCaseDescription(request.caseDescription());
        }

        if (request.matterDescription() != null && !Objects.equals(request.matterDescription(), caseEntity.getMatterDescription())) {
            changedFields.add("matterDescription");
            caseEntity.setMatterDescription(request.matterDescription());
        }

        if (!Objects.equals(request.priority(), caseEntity.getPriority())) {
            changedFields.add("priority");
            caseEntity.setPriority(request.priority() != null ? request.priority() : CasePriority.NORMAL);
        }

        if (!Objects.equals(request.opposingParty(), caseEntity.getOpposingParty())) {
            changedFields.add("opposingParty");
            caseEntity.setOpposingParty(request.opposingParty());
        }

        if (!Objects.equals(request.outcome(), caseEntity.getOutcome())) {
            changedFields.add("outcome");
            caseEntity.setOutcome(request.outcome());
        }

        if (!Objects.equals(request.outcomeNotes(), caseEntity.getOutcomeNotes())) {
            changedFields.add("outcomeNotes");
            caseEntity.setOutcomeNotes(request.outcomeNotes());
        }

        if (!Objects.equals(request.initialPaymentDate(), caseEntity.getInitialPaymentDate())) {
            changedFields.add("initialPaymentDate");
            caseEntity.setInitialPaymentDate(request.initialPaymentDate());
        }

        if (!Objects.equals(request.fiscalYear(), caseEntity.getFiscalYear())) {
            changedFields.add("fiscalYear");
            caseEntity.setFiscalYear(request.fiscalYear());
        }

        if (request.parentCaseId() != null) {
            if (caseEntity.getParentCase() == null || !request.parentCaseId().equals(caseEntity.getParentCase().getId())) {
                Case parent = caseRepository.findById(request.parentCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent case not found with id: " + request.parentCaseId()));
                changedFields.add("parentCase");
                caseEntity.setParentCase(parent);
            }
        }

        if (request.clientId() != null) {
            Long currentClientId = caseEntity.getClient() != null ? caseEntity.getClient().getId() : null;
            if (!request.clientId().equals(currentClientId)) {
                Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + request.clientId()));
                changedFields.add("client");
                caseEntity.setClient(client);
            }
        }

        caseEntity = caseRepository.save(caseEntity);

        if (!changedFields.isEmpty()) {
            CaseResponse afterSnapshot = caseMapper.toResponse(caseEntity);
            auditLogService.log("CASE", id, "CASE_UPDATED", beforeSnapshot, afterSnapshot, getCurrentUsername());
        }

        return caseMapper.toResponse(caseEntity);
    }

    @Transactional
    public CaseResponse assignClient(Long caseId, Long clientId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + clientId));
            caseEntity.setClient(client);
        } else {
            caseEntity.setClient(null);
        }
        return caseMapper.toResponse(caseRepository.save(caseEntity));
    }

    @Transactional
    public CaseResponse changeStatus(Long id, ChangeStatusRequest request, UserPrincipal currentUser) {
        Case caseEntity = caseRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        CaseStatus currentStatus = caseEntity.getStatus();
        String newStatusCode = request.statusCode();

        if (currentStatus.getCode().equals("ARCHIVED")) {
            throw new InvalidStatusTransitionException("ARCHIVED cases cannot change status.");
        }
        if (currentStatus.getCode().equals("CLOSED") && !newStatusCode.equals("ARCHIVED")) {
            throw new InvalidStatusTransitionException("CLOSED cases can only be archived.");
        }

        CaseStatus newStatus = caseStatusRepository.findByCode(newStatusCode)
            .orElseThrow(() -> new ResourceNotFoundException("CaseStatus not found with code: " + newStatusCode));

        if (!caseEntity.getCaseType().getAllowedStatuses().contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Status " + newStatus.getCode() + " is not allowed for case type " +
                caseEntity.getCaseType().getCode()
            );
        }

        caseEntity.setStatus(newStatus);
        caseEntity = caseRepository.save(caseEntity);

        auditLogService.log("CASE", id, "CASE_STATUS_CHANGED",
            List.of("status", "statusReason"), getCurrentUsername());

        return caseMapper.toResponse(caseEntity);
    }

    @Transactional
    public void deleteCase(Long id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        if (Boolean.TRUE.equals(caseEntity.getStatus().getIsTerminal())) {
            throw new BusinessRuleException(
                "Cases with status '" + caseEntity.getStatus().getCode()
                + "' cannot be deleted. Archive them instead."
            );
        }

        caseEntity.setDeletedAt(LocalDateTime.now());
        caseRepository.save(caseEntity);

        auditLogService.log("CASE", id, "CASE_DELETED", List.of("deletedAt"), getCurrentUsername());
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
