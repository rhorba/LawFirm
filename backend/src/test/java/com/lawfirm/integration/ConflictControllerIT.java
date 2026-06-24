package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConflictControllerIT extends BaseIntegrationTest {

    @Test
    void performCheck_ShouldReturn200_WithResult() throws Exception {
        Map<String, Object> body = Map.of("searchName", "NonExistentPartyXYZ12345");

        mockMvc.perform(post("/api/conflicts/check")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.searchName").value("NonExistentPartyXYZ12345"))
            .andExpect(jsonPath("$.hasConflict").value(false))
            .andExpect(jsonPath("$.matches").isArray());
    }

    @Test
    void listChecks_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/conflicts/history")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void performCheck_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        Map<String, Object> body = Map.of("searchName", "Test");

        mockMvc.perform(post("/api/conflicts/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized());
    }
}
