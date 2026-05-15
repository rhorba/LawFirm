package com.lawfirm.application.dto.response;

import java.math.BigDecimal;

public record TimeEntrySummaryResponse(
    BigDecimal totalHours,
    BigDecimal billableHours,
    BigDecimal billedHours,
    BigDecimal unbilledHours,
    BigDecimal billableAmount,
    BigDecimal billedAmount,
    BigDecimal unbilledAmount
) {}
