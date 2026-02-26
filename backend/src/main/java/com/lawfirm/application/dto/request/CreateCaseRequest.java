package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.CaseOutcome;
import com.lawfirm.domain.model.CasePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateCaseRequest(
    @NotBlank(message = "Case type code is required")
    @Size(max = 20)
    String caseTypeCode,

    @Size(max = 10)
    String caseCategoryCode,

    @NotBlank(message = "Tribunal code is required")
    @Size(max = 50)
    String tribunalCode,

    @NotEmpty(message = "At least one lawyer must be assigned")
    Set<Long> lawyerIds,

    @PastOrPresent(message = "Registration date cannot be in the future")
    LocalDate registrationDate,

    @NotBlank(message = "Case description is required")
    @Size(max = 500, message = "Case description must not exceed 500 characters")
    String caseDescription,

    @Size(max = 1000, message = "Matter description must not exceed 1000 characters")
    String matterDescription,

    @Size(max = 50)
    String initialStatusCode,

    CasePriority priority,

    @Size(max = 255)
    String opposingParty,

    CaseOutcome outcome,

    @Size(max = 1000)
    String outcomeNotes,

    LocalDate initialPaymentDate,

    Short fiscalYear,

    Long parentCaseId
) {}
