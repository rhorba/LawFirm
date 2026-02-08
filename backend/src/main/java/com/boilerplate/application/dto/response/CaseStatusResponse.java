package com.boilerplate.application.dto.response;

public record CaseStatusResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    Integer sortOrder,
    Boolean isTerminal
) {}
