package com.lawfirm.application.dto.response;

public record CaseTemplateResponse(
    Long id,
    String name,
    String caseTypeCode,
    String caseCategoryCode
) {}
