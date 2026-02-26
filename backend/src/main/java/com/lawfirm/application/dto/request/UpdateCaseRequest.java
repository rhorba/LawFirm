package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.CaseOutcome;
import com.lawfirm.domain.model.CasePriority;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record UpdateCaseRequest(
    @Size(max = 50)
    String tribunalCode,

    @Size(max = 10)
    String caseCategoryCode,

    @NotEmpty(message = "At least one lawyer must be assigned")
    Set<Long> lawyerIds,

    @PastOrPresent
    LocalDate registrationDate,

    @Size(max = 500)
    String caseDescription,

    @Size(max = 1000)
    String matterDescription,

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
