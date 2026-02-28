package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.model.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClientResponse(
    Long id,
    String fullName,
    ClientType clientType,
    String firstName,
    String lastName,
    String phone,
    String email,
    String address,
    String notes,
    String cin,
    Gender gender,
    LocalDate dateOfBirth,
    Integer age,
    String companyName,
    String taxNumber,
    Boolean active,
    int caseCount,
    LocalDateTime createdAt
) {}
