package com.lawfirm.application.dto.response;

import java.math.BigDecimal;

public record ReportSummaryResponse(
    long totalCases,
    long openCases,
    long overdueTasks,
    BigDecimal totalRevenue,
    BigDecimal unbilledHours,
    BigDecimal unbilledAmount,
    long totalDocuments,
    long pendingInvoices
) {}
