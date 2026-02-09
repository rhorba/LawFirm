package com.lawfirm.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    LawyerResponse lawyer,
    CaseStatusResponse status,

    FinancialSummary financialSummary
) {}
