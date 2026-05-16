package com.lawfirm.application.dto.response;

import java.math.BigDecimal;

public record LawyerWorkloadEntry(
    Long lawyerId,
    String lawyerName,
    long caseCount,
    BigDecimal totalHours,
    BigDecimal billableHours,
    BigDecimal billableAmount
) {}
