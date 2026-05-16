package com.lawfirm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UnpaidInvoiceEntry(
    Long invoiceId,
    String invoiceNumber,
    Long caseId,
    String caseNumber,
    BigDecimal totalAmount,
    LocalDate dueDate,
    String status
) {}
