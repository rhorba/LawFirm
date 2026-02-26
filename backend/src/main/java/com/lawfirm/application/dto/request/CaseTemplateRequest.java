package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseTemplateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 20)  String caseTypeCode,
    @NotBlank @Size(max = 20)  String caseCategoryCode
) {}
