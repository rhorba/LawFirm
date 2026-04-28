package com.lawfirm.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
    @NotNull Long caseId,
    @NotNull LocalDate issueDate,
    LocalDate dueDate,
    @DecimalMin("0.00") BigDecimal taxAmount,
    String notes,
    @NotEmpty @Valid List<InvoiceItemRequest> items
) {}
