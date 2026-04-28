package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.InvoiceRequest;
import com.lawfirm.application.dto.request.InvoiceStatusRequest;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.application.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice lifecycle management")
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'INVOICE_READ')")
    @Operation(summary = "Paginated invoice list (non-deleted)")
    public ResponseEntity<Page<InvoiceResponse>> list(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            service.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'INVOICE_CREATE')")
    @Operation(summary = "Create an invoice with line items")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'INVOICE_READ')")
    @Operation(summary = "Get invoice detail with items")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'INVOICE_MANAGE')")
    @Operation(summary = "Transition invoice status (DRAFT→SENT→PAID, DRAFT/SENT→CANCELLED)")
    public ResponseEntity<InvoiceResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody InvoiceStatusRequest request
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'INVOICE_MANAGE')")
    @Operation(summary = "Soft-delete an invoice")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
