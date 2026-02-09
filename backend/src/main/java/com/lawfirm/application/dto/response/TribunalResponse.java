package com.lawfirm.application.dto.response;

public record TribunalResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    Boolean active
) {}
