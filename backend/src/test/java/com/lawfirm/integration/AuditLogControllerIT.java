package com.lawfirm.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLogControllerIT extends BaseIntegrationTest {

    @Test
    void getAllAuditLogs_ShouldReturn200_WithPagedResult() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllAuditLogs_WithPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                .param("page", "0")
                .param("size", "5")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getAllAuditLogs_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
            .andExpect(status().isUnauthorized());
    }
}
