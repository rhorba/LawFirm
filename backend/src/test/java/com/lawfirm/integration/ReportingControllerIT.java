package com.lawfirm.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportingControllerIT extends BaseIntegrationTest {

    @Test
    void getSummary_ShouldReturn200_WithKpiFields() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCases").isNumber())
            .andExpect(jsonPath("$.openCases").isNumber())
            .andExpect(jsonPath("$.overdueTasks").isNumber())
            .andExpect(jsonPath("$.totalRevenue").isNumber())
            .andExpect(jsonPath("$.pendingInvoices").isNumber());
    }

    @Test
    void getCasesByStatus_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/reports/cases-by-status")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labels").isArray())
            .andExpect(jsonPath("$.values").isArray());
    }

    @Test
    void getFinancialByMonth_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/reports/financial-by-month")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labels").isArray());
    }

    @Test
    void getLawyerWorkload_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/reports/lawyer-workload")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getTopUnpaidInvoices_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/reports/top-unpaid-invoices")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSummary_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31"))
            .andExpect(status().isUnauthorized());
    }
}
