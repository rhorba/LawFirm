package com.boilerplate.application.dto.response;

import java.util.List;

public record CaseTypeResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    String numberFormatTemplate,
    Boolean active,
    List<CaseStatusResponse> allowedStatuses
) {}
