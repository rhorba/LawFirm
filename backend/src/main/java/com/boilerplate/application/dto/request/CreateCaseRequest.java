package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCaseRequest(
    @NotBlank(message = "Case type code is required")
    @Size(max = 20)
    String caseTypeCode,

    @Size(max = 10)
    String caseCategoryCode,

    @NotBlank(message = "Tribunal code is required")
    @Size(max = 50)
    String tribunalCode,

    @NotNull(message = "Lawyer ID is required")
    Long lawyerId,

    @NotNull(message = "Registration date is required")
    @PastOrPresent(message = "Registration date cannot be in the future")
    LocalDate registrationDate,

    @NotBlank(message = "Case description is required")
    @Size(max = 500, message = "Case description must not exceed 500 characters")
    String caseDescription,

    @Size(max = 1000, message = "Matter description must not exceed 1000 characters")
    String matterDescription,

    @Size(max = 50)
    String initialStatusCode
) {}
