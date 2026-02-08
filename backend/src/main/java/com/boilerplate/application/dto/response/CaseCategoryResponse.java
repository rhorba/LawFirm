package com.boilerplate.application.dto.response;

public record CaseCategoryResponse(
    Long id,
    String code,
    String nameAr,
    String nameFr,
    String caseTypeCode
) {}
