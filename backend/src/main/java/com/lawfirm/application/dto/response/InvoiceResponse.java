package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.Invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    Long caseId,
    String caseNumber,
    String invoiceNumber,
    LocalDate issueDate,
    LocalDate dueDate,
    InvoiceStatus status,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    String notes,
    List<InvoiceItemResponse> items,
    LocalDateTime createdAt
) {}
