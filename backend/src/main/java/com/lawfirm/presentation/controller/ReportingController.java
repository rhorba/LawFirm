package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.response.ChartDataResponse;
import com.lawfirm.application.dto.response.LawyerWorkloadEntry;
import com.lawfirm.application.dto.response.ReportSummaryResponse;
import com.lawfirm.application.dto.response.UnpaidInvoiceEntry;
import com.lawfirm.application.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/summary")
    @PreAuthorize("hasPermission(null, 'REPORT_READ')")
    public ResponseEntity<ReportSummaryResponse> getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.getSummary(resolveFrom(from), resolveTo(to)));
    }

    @GetMapping("/cases-by-status")
    @PreAuthorize("hasPermission(null, 'REPORT_READ')")
    public ResponseEntity<ChartDataResponse> getCasesByStatus(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.getCasesByStatus(resolveFrom(from), resolveTo(to)));
    }

    @GetMapping("/cases-by-month")
    @PreAuthorize("hasPermission(null, 'REPORT_READ')")
    public ResponseEntity<ChartDataResponse> getCasesByMonth(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.getCasesByMonth(resolveFrom(from), resolveTo(to)));
    }

    @GetMapping("/financial-by-month")
    @PreAuthorize("hasPermission(null, 'REPORT_READ')")
    public ResponseEntity<ChartDataResponse> getFinancialByMonth(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.getFinancialByMonth(resolveFrom(from), resolveTo(to)));
    }

    @GetMapping("/lawyer-workload")
    @PreAuthorize("hasPermission(null, 'REPORT_READ')")
    public ResponseEntity<List<LawyerWorkloadEntry>> getLawyerWorkload(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.getLawyerWorkload(resolveFrom(from), resolveTo(to)));
    }

    @GetMapping("/unpaid-invoices")
    @PreAuthorize("hasPermission(null, 'REPORT_READ')")
    public ResponseEntity<List<UnpaidInvoiceEntry>> getUnpaidInvoices() {
        return ResponseEntity.ok(reportingService.getTopUnpaidInvoices());
    }

    private LocalDate resolveFrom(LocalDate from) {
        return from != null ? from : LocalDate.now().minusYears(1);
    }

    private LocalDate resolveTo(LocalDate to) {
        return to != null ? to : LocalDate.now();
    }
}
