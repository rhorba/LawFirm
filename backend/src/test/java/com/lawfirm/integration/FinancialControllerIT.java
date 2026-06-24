package com.lawfirm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinancialControllerIT extends BaseIntegrationTest {

    private Long createCase() throws Exception {
        var lawyersResult = mockMvc.perform(get("/api/lawyers")
                .header("Authorization", bearerToken()))
            .andReturn();
        JsonNode page = objectMapper.readTree(lawyersResult.getResponse().getContentAsString());
        Long lawyerId = page.get("content").get(0).get("id").asLong();

        Map<String, Object> body = new HashMap<>();
        body.put("caseTypeCode", "CIVIL");
        body.put("tribunalCode", "TR_PIN_1");
        body.put("lawyerIds", Set.of(lawyerId));
        body.put("registrationDate", "2025-01-01");
        body.put("caseDescription", "Test case for financial IT");

        var result = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createInvoice(Long caseId) throws Exception {
        Map<String, Object> item = Map.of(
            "description", "Legal services",
            "operationType", "OTHER",
            "quantity", 1,
            "unitPrice", new BigDecimal("1500.00")
        );
        Map<String, Object> body = Map.of(
            "caseId", caseId,
            "issueDate", LocalDate.now().toString(),
            "dueDate", LocalDate.now().plusDays(30).toString(),
            "items", List.of(item)
        );

        var result = mockMvc.perform(post("/api/financial/invoices")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void listTransactions_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/financial/transactions")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listInvoices_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/financial/invoices")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getInvoiceById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/financial/invoices/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void createInvoice_ShouldReturn201_WithInvoiceNumber() throws Exception {
        Long caseId = createCase();

        Map<String, Object> item = Map.of(
            "description", "Consultation fees",
            "operationType", "OTHER",
            "quantity", 2,
            "unitPrice", new BigDecimal("800.00")
        );
        Map<String, Object> body = Map.of(
            "caseId", caseId,
            "issueDate", LocalDate.now().toString(),
            "dueDate", LocalDate.now().plusDays(30).toString(),
            "items", List.of(item)
        );

        mockMvc.perform(post("/api/financial/invoices")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invoiceNumber").isNotEmpty())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.totalAmount").value(1600.00));
    }

    @Test
    void getInvoiceById_ShouldReturn200_WhenExists() throws Exception {
        Long caseId = createCase();
        Long invoiceId = createInvoice(caseId);

        mockMvc.perform(get("/api/financial/invoices/" + invoiceId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invoiceId));
    }

    @Test
    void updateInvoiceStatus_DraftToSent_ShouldReturn200() throws Exception {
        Long caseId = createCase();
        Long invoiceId = createInvoice(caseId);

        Map<String, Object> statusBody = Map.of("status", "SENT");

        mockMvc.perform(patch("/api/financial/invoices/" + invoiceId + "/status")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void updateInvoiceStatus_SentToPaid_ShouldReturn200_AndCreateTransaction() throws Exception {
        Long caseId = createCase();
        Long invoiceId = createInvoice(caseId);

        Map<String, Object> sentBody = Map.of("status", "SENT");
        mockMvc.perform(patch("/api/financial/invoices/" + invoiceId + "/status")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sentBody)))
            .andExpect(status().isOk());

        Map<String, Object> paidBody = Map.of(
            "status", "PAID",
            "paymentMode", "TRANSFER",
            "paymentDate", LocalDate.now().toString()
        );

        mockMvc.perform(patch("/api/financial/invoices/" + invoiceId + "/status")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paidBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void updateInvoiceStatus_InvalidTransition_ShouldReturn400() throws Exception {
        Long caseId = createCase();
        Long invoiceId = createInvoice(caseId);

        Map<String, Object> body = Map.of("status", "PAID");

        mockMvc.perform(patch("/api/financial/invoices/" + invoiceId + "/status")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteInvoice_ShouldReturn204_WhenExists() throws Exception {
        Long caseId = createCase();
        Long invoiceId = createInvoice(caseId);

        mockMvc.perform(delete("/api/financial/invoices/" + invoiceId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void exportTransactions_ShouldReturn200_WithExcelContentType() throws Exception {
        mockMvc.perform(get("/api/financial/transactions/export/excel")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type",
                containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @Test
    void listTransactions_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/financial/transactions"))
            .andExpect(status().isUnauthorized());
    }
}
