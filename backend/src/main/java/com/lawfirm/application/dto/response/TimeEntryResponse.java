package com.lawfirm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TimeEntryResponse(
    Long id,
    Long caseId,
    String caseNumber,
    Long lawyerId,
    String lawyerFullName,
    LocalDate entryDate,
    BigDecimal hours,
    String description,
    BigDecimal hourlyRate,
    BigDecimal billableAmount,
    boolean billable,
    boolean billed,
    Long invoiceId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
