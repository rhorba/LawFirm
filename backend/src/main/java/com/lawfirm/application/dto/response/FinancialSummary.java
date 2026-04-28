package com.lawfirm.application.dto.response;

import java.math.BigDecimal;

public record FinancialSummary(
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal balance,
    Integer transactionCount
) {}
