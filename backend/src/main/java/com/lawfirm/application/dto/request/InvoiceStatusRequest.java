package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.model.Invoice;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record InvoiceStatusRequest(
    @NotNull Invoice.InvoiceStatus status,
    FinancialTransaction.PaymentMode paymentMode,  // required when status = PAID
    LocalDate paymentDate,                          // required when status = PAID
    String paymentReference                         // optional
) {}
