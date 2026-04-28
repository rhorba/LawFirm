package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;

import java.time.LocalDate;

public record FinancialFilterRequest(
    Long caseId,
    Long clientId,
    Direction direction,
    OperationType operationType,
    LocalDate dateFrom,
    LocalDate dateTo
) {}
