package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.CaseOutcome;
import com.lawfirm.domain.model.CasePriority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CaseResponse(
    Long id,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    Integer year,
    Integer sequenceNumber,
    String fullCaseNumber,
    LocalDate registrationDate,
    String caseDescription,
    String matterDescription,

    TribunalResponse tribunal,
    CaseTypeResponse caseType,
    CaseCategoryResponse caseCategory,
    List<LawyerResponse> lawyers,
    CaseStatusResponse status,

    Long clientId,
    String clientName,

    CasePriority priority,
    String opposingParty,
    CaseOutcome outcome,
    String outcomeNotes,
    LocalDate initialPaymentDate,
    Short fiscalYear,
    CaseSummaryResponse parentCase,

    FinancialSummary financialSummary
) {}
