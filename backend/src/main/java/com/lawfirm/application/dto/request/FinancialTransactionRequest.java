package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import com.lawfirm.domain.model.FinancialTransaction.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionRequest(
    @NotNull Long caseId,
    @NotNull Direction direction,
    @NotNull OperationType operationType,
    PaymentMode paymentMode,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    LocalDate paymentDate,
    String paymentReference,
    String accountNumber,
    String description
) {}
