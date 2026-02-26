package com.lawfirm.application.service;

import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.CaseSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseExportService {

    private final CaseRepository caseRepository;

    private static final String[] HEADERS = {
        "Case Number", "Type", "Category", "Tribunal",
        "Lawyers", "Priority", "Status", "Outcome",
        "Opposing Party", "Registration Date", "Fiscal Year", "Initial Payment Date"
    };

    @Transactional(readOnly = true)
    public byte[] exportFilteredCases(
        Integer year,
        String caseTypeCode,
        String categoryCode,
        String tribunalCode,
        Long lawyerId,
        String statusCode,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo,
        String search
    ) {
        Specification<Case> spec = CaseSpecification.withFilters(
            year, caseTypeCode, categoryCode, tribunalCode, lawyerId, statusCode, null,
            registrationDateFrom, registrationDateTo, search
        );
        List<Case> cases = caseRepository.findAll(spec);
        return buildExcel(cases);
    }

    private byte[] buildExcel(List<Case> cases) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cases");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Case c : cases) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getFullCaseNumber());
                row.createCell(1).setCellValue(c.getCaseType() != null ? c.getCaseType().getNameFr() : "");
                row.createCell(2).setCellValue(c.getCaseCategory() != null ? c.getCaseCategory().getNameFr() : "");
                row.createCell(3).setCellValue(c.getTribunal() != null ? c.getTribunal().getNameFr() : "");
                row.createCell(4).setCellValue(
                    c.getLawyers() != null
                        ? c.getLawyers().stream()
                            .map(l -> l.getFirstName() + " " + l.getLastName())
                            .collect(Collectors.joining(", "))
                        : ""
                );
                row.createCell(5).setCellValue(c.getPriority() != null ? c.getPriority().name() : "");
                row.createCell(6).setCellValue(c.getStatus() != null ? c.getStatus().getCode() : "");
                row.createCell(7).setCellValue(c.getOutcome() != null ? c.getOutcome().name() : "");
                row.createCell(8).setCellValue(c.getOpposingParty() != null ? c.getOpposingParty() : "");
                row.createCell(9).setCellValue(c.getRegistrationDate() != null ? c.getRegistrationDate().toString() : "");
                row.createCell(10).setCellValue(c.getFiscalYear() != null ? c.getFiscalYear().toString() : "");
                row.createCell(11).setCellValue(c.getInitialPaymentDate() != null ? c.getInitialPaymentDate().toString() : "");
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }
}
