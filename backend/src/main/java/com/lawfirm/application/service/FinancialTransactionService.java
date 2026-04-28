package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.FinancialFilterRequest;
import com.lawfirm.application.dto.request.FinancialTransactionRequest;
import com.lawfirm.application.dto.response.FinancialSummary;
import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.application.mapper.FinancialTransactionMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.FinancialTransactionRepository;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final CaseRepository caseRepository;
    private final FinancialTransactionMapper mapper;

    private static final String[] EXPORT_HEADERS = {
        "ID", "Case", "Direction", "Operation Type", "Payment Mode",
        "Amount (MAD)", "Payment Date", "Reference", "Description", "Created At"
    };

    // ── Read ─────────────────────────────────────────────────────────────────

    public Page<FinancialTransactionResponse> search(FinancialFilterRequest filter, Pageable pageable) {
        return transactionRepository.findAll(buildSpec(filter), pageable)
            .map(mapper::toResponse);
    }

    public List<FinancialTransactionResponse> findByCaseId(Long caseId) {
        return transactionRepository.findByCaseEntityIdAndDeletedAtIsNull(caseId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    public FinancialSummary getSummaryByCaseId(Long caseId) {
        BigDecimal revenue  = transactionRepository.sumRevenueByCaseId(caseId);
        BigDecimal expenses = transactionRepository.sumExpensesByCaseId(caseId);
        int count           = transactionRepository.countByCaseId(caseId);
        return new FinancialSummary(revenue, expenses, revenue.subtract(expenses), count);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionResponse create(FinancialTransactionRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.caseId()));

        FinancialTransaction tx = FinancialTransaction.builder()
            .caseEntity(caseEntity)
            .direction(request.direction())
            .operationType(request.operationType())
            .paymentMode(request.paymentMode())
            .amount(request.amount())
            .paymentDate(request.paymentDate())
            .paymentReference(request.paymentReference())
            .accountNumber(request.accountNumber())
            .description(request.description())
            .build();

        return mapper.toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public void softDelete(Long id) {
        FinancialTransaction tx = transactionRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        tx.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    public byte[] exportExcel(FinancialFilterRequest filter) {
        List<FinancialTransaction> transactions = transactionRepository.findAll(buildSpec(filter));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (FinancialTransaction t : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getId());
                row.createCell(1).setCellValue(t.getCaseEntity().getFullCaseNumber());
                row.createCell(2).setCellValue(t.getDirection().name());
                row.createCell(3).setCellValue(t.getOperationType().name());
                row.createCell(4).setCellValue(t.getPaymentMode() != null ? t.getPaymentMode().name() : "");
                row.createCell(5).setCellValue(t.getAmount().doubleValue());
                row.createCell(6).setCellValue(t.getPaymentDate() != null ? t.getPaymentDate().toString() : "");
                row.createCell(7).setCellValue(t.getPaymentReference() != null ? t.getPaymentReference() : "");
                row.createCell(8).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(9).setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate transaction Excel export", e);
        }
    }

    // ── Specification builder ─────────────────────────────────────────────────

    private Specification<FinancialTransaction> buildSpec(FinancialFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (f.caseId() != null) {
                predicates.add(cb.equal(root.get("caseEntity").get("id"), f.caseId()));
            }
            if (f.clientId() != null) {
                predicates.add(cb.equal(root.get("caseEntity").get("client").get("id"), f.clientId()));
            }
            if (f.direction() != null) {
                predicates.add(cb.equal(root.get("direction"), f.direction()));
            }
            if (f.operationType() != null) {
                predicates.add(cb.equal(root.get("operationType"), f.operationType()));
            }
            if (f.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), f.dateFrom()));
            }
            if (f.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), f.dateTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
