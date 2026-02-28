package com.lawfirm.application.service;

import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientExportService {

    private final ClientRepository clientRepository;

    private static final String[] HEADERS = {
        "Full Name", "Type", "CIN", "Company Name", "Tax Number",
        "Phone", "Email", "Address", "Active", "Case Count", "Date of Birth", "Registered At"
    };

    @Transactional(readOnly = true)
    public byte[] export(String search, ClientType type) {
        String term = (search != null && search.isBlank()) ? null : search;
        List<Client> clients = clientRepository
            .search(term, type, Pageable.unpaged())
            .getContent();
        return buildExcel(clients);
    }

    private byte[] buildExcel(List<Client> clients) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clients");

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
            for (Client c : clients) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getFullName() != null ? c.getFullName() : "");
                row.createCell(1).setCellValue(c.getClientType().name());
                row.createCell(2).setCellValue(c.getCin() != null ? c.getCin() : "");
                row.createCell(3).setCellValue(c.getCompanyName() != null ? c.getCompanyName() : "");
                row.createCell(4).setCellValue(c.getTaxNumber() != null ? c.getTaxNumber() : "");
                row.createCell(5).setCellValue(c.getPhone() != null ? c.getPhone() : "");
                row.createCell(6).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                row.createCell(7).setCellValue(c.getAddress() != null ? c.getAddress() : "");
                row.createCell(8).setCellValue(Boolean.TRUE.equals(c.getActive()) ? "Yes" : "No");
                row.createCell(9).setCellValue(c.getCases().size());
                row.createCell(10).setCellValue(c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "");
                row.createCell(11).setCellValue(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < HEADERS.length; i++) { sheet.autoSizeColumn(i); }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate client Excel export", e);
        }
    }
}
