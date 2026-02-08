package com.boilerplate.application.dto.response;

public record LawyerResponse(
    Long id,
    String firstName,
    String lastName,
    String fullName,
    String taxId,
    String email,
    String phone,
    Boolean active
) {}
