package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.InvoiceItemRequest;
import com.lawfirm.application.dto.request.InvoiceRequest;
import com.lawfirm.application.dto.request.InvoiceStatusRequest;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.application.mapper.InvoiceMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.Invoice.InvoiceStatus;
import com.lawfirm.domain.model.InvoiceItem;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.FinancialTransactionRepository;
import com.lawfirm.domain.repository.InvoiceRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CaseRepository caseRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final InvoiceMapper mapper;
    private final EntityManager entityManager;


    // ── Read ─────────────────────────────────────────────────────────────────

    public Page<InvoiceResponse> findAll(Pageable pageable) {
        return invoiceRepository.findAllByDeletedAtIsNull(pageable).map(mapper::toResponse);
    }

    public InvoiceResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.caseId()));

        String invoiceNumber = generateInvoiceNumber(request.issueDate().getYear());

        List<InvoiceItem> items = request.items().stream()
            .map(this::buildItem)
            .toList();

        BigDecimal subtotal = items.stream()
            .map(InvoiceItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount   = request.taxAmount() != null ? request.taxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxAmount);

        Invoice invoice = Invoice.builder()
            .caseEntity(caseEntity)
            .invoiceNumber(invoiceNumber)
            .issueDate(request.issueDate())
            .dueDate(request.dueDate())
            .notes(request.notes())
            .subtotal(subtotal)
            .taxAmount(taxAmount)
            .totalAmount(totalAmount)
            .build();

        items.forEach(item -> item.setInvoice(invoice));
        invoice.getItems().addAll(items);

        return mapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatusRequest request) {
        Invoice invoice = getOrThrow(id);
        InvoiceStatus current = invoice.getStatus();
        InvoiceStatus next = request.status();

        if (!isValidTransition(current, next)) {
            throw new IllegalArgumentException(
                "Cannot transition invoice from " + current + " to " + next);
        }

        if (next == InvoiceStatus.PAID) {
            if (request.paymentMode() == null || request.paymentDate() == null) {
                throw new IllegalArgumentException(
                    "paymentMode and paymentDate are required when marking invoice as PAID");
            }
            FinancialTransaction tx = new FinancialTransaction();
            tx.setCaseEntity(invoice.getCaseEntity());
            tx.setInvoice(invoice);
            tx.setDirection(FinancialTransaction.Direction.REVENUE);
            tx.setOperationType(FinancialTransaction.OperationType.OTHER);
            tx.setPaymentMode(request.paymentMode());
            tx.setAmount(invoice.getTotalAmount());
            tx.setPaymentDate(request.paymentDate());
            tx.setPaymentReference(request.paymentReference());
            tx.setDescription("Paiement facture #" + invoice.getInvoiceNumber());
            financialTransactionRepository.save(tx);
        }

        invoice.setStatus(next);
        return mapper.toResponse(invoiceRepository.save(invoice));
    }

    private boolean isValidTransition(InvoiceStatus from, InvoiceStatus to) {
        return switch (from) {
            case DRAFT -> to == InvoiceStatus.SENT || to == InvoiceStatus.CANCELLED;
            case SENT  -> to == InvoiceStatus.PAID || to == InvoiceStatus.CANCELLED;
            default    -> false;
        };
    }

    @Transactional
    public void softDelete(Long id) {
        Invoice invoice = getOrThrow(id);
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice getOrThrow(Long id) {
        return invoiceRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private InvoiceItem buildItem(InvoiceItemRequest req) {
        BigDecimal lineTotal = req.unitPrice().multiply(BigDecimal.valueOf(req.quantity()));
        return InvoiceItem.builder()
            .description(req.description())
            .operationType(req.operationType())
            .quantity(req.quantity())
            .unitPrice(req.unitPrice())
            .lineTotal(lineTotal)
            .build();
    }

    @SuppressWarnings("unchecked")
    private synchronized String generateInvoiceNumber(int year) {
        Long seq = ((Number) entityManager
            .createNativeQuery("SELECT nextval('invoice_number_seq')")
            .getSingleResult()).longValue();
        return String.format("FAC-%d-%04d", year, seq);
    }
}
