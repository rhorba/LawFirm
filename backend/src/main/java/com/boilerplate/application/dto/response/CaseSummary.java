package com.boilerplate.application.dto.response;

import java.time.LocalDate;

public record CaseSummary(
    Long id,
    String fullCaseNumber,
    String caseDescription,
    String tribunalNameFr,
    String caseTypeNameFr,
    String lawyerName,
    String statusNameFr,
    LocalDate registrationDate
) {}
