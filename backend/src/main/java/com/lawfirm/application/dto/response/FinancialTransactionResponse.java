package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import com.lawfirm.domain.model.FinancialTransaction.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialTransactionResponse(
    Long id,
    Long caseId,
    String caseNumber,
    Long invoiceId,
    Direction direction,
    OperationType operationType,
    PaymentMode paymentMode,
    BigDecimal amount,
    LocalDate paymentDate,
    String paymentReference,
    String accountNumber,
    String description,
    LocalDateTime createdAt
) {}
