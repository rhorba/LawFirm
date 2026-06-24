package com.lawfirm.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReferenceDataControllerIT extends BaseIntegrationTest {

    // ── Case Categories ───────────────────────────────────────────────────────

    @Test
    void getCaseCategories_ShouldReturn200_WithPagedResult() throws Exception {
        mockMvc.perform(get("/api/case-categories")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getCaseCategories_WithCaseTypeFilter_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/case-categories")
                .param("caseTypeCode", "CIVIL")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getCaseCategories_WithSearch_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/case-categories")
                .param("search", "civil")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    // ── Case Statuses ─────────────────────────────────────────────────────────

    @Test
    void getCaseStatuses_ShouldReturn200_WithList() throws Exception {
        mockMvc.perform(get("/api/case-statuses")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    @Test
    void getRoles_ShouldReturn200_WithList() throws Exception {
        mockMvc.perform(get("/api/roles")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    @Test
    void getPermissions_ShouldReturn200_WithList() throws Exception {
        mockMvc.perform(get("/api/permissions")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ── Case Types ────────────────────────────────────────────────────────────

    @Test
    void getCaseTypes_ShouldReturn200_WithList() throws Exception {
        mockMvc.perform(get("/api/case-types")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk());
    }
}
