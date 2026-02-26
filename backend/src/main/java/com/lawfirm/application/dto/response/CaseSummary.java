package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.CasePriority;

import java.time.LocalDate;

public record CaseSummary(
    Long id,
    String fullCaseNumber,
    String caseDescription,
    String tribunalNameFr,
    String caseTypeNameFr,
    String lawyerName,
    String statusNameFr,
    LocalDate registrationDate,
    CasePriority priority
) {}
