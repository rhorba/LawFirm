package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinancialControllerIT extends BaseIntegrationTest {

    @Test
    void listTransactions_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/financial/transactions")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listInvoices_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/invoices")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getInvoiceById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/invoices/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void exportTransactions_ShouldReturn200_WithExcelContentType() throws Exception {
        mockMvc.perform(get("/api/financial/transactions/export")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type",
                containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }
}
