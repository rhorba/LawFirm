package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvoiceItemRequest(
    @NotBlank @Size(max = 255) String description,
    @NotNull OperationType operationType,
    @Min(1) int quantity,
    @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {}
