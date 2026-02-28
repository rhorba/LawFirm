package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.ClientType;

public record ClientSummary(
    Long id,
    String fullName,
    ClientType clientType,
    String cin,
    String taxNumber,
    String phone,
    String email,
    Boolean active,
    int caseCount
) {}
