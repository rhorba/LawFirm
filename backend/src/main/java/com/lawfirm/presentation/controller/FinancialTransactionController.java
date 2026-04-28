package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.FinancialFilterRequest;
import com.lawfirm.application.dto.request.FinancialTransactionRequest;
import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.application.service.FinancialTransactionService;
import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Tag(name = "Financial Transactions", description = "Financial ledger management")
public class FinancialTransactionController {

    private final FinancialTransactionService service;

    @GetMapping("/transactions")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_READ')")
    @Operation(summary = "Paginated transaction list with optional filters")
    public ResponseEntity<Page<FinancialTransactionResponse>> list(
        @RequestParam(required = false) Long caseId,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) Direction direction,
        @RequestParam(required = false) OperationType operationType,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort
    ) {
        FinancialFilterRequest filter = new FinancialFilterRequest(
            caseId, clientId, direction, operationType,
            dateFrom != null ? LocalDate.parse(dateFrom) : null,
            dateTo   != null ? LocalDate.parse(dateTo)   : null
        );
        return ResponseEntity.ok(
            service.search(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort)))
        );
    }

    @GetMapping("/cases/{caseId}/transactions")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_READ')")
    @Operation(summary = "All non-deleted transactions for a specific case")
    public ResponseEntity<List<FinancialTransactionResponse>> listByCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(service.findByCaseId(caseId));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_CREATE')")
    @Operation(summary = "Create a new financial transaction")
    public ResponseEntity<FinancialTransactionResponse> create(
        @Valid @RequestBody FinancialTransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_UPDATE')")
    @Operation(summary = "Soft-delete a transaction")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions/export/excel")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_READ')")
    @Operation(summary = "Export filtered transactions as Excel")
    public void exportExcel(
        @RequestParam(required = false) Long caseId,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) Direction direction,
        @RequestParam(required = false) OperationType operationType,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        HttpServletResponse response
    ) throws IOException {
        FinancialFilterRequest filter = new FinancialFilterRequest(
            caseId, clientId, direction, operationType,
            dateFrom != null ? LocalDate.parse(dateFrom) : null,
            dateTo   != null ? LocalDate.parse(dateTo)   : null
        );
        byte[] xlsx = service.exportExcel(filter);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");
        response.getOutputStream().write(xlsx);
    }
}
