package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.FinancialTransaction.OperationType;

import java.math.BigDecimal;

public record InvoiceItemResponse(
    Long id,
    String description,
    OperationType operationType,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {}
