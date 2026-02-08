package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCaseRequest(
    @Size(max = 50)
    String tribunalCode,

    @Size(max = 10)
    String caseCategoryCode,

    Long lawyerId,

    @PastOrPresent
    LocalDate registrationDate,

    @Size(max = 500)
    String caseDescription,

    @Size(max = 1000)
    String matterDescription
) {}
