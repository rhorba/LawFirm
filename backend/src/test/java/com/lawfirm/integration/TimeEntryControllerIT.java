package com.lawfirm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TimeEntryControllerIT extends BaseIntegrationTest {

    private Long getFirstLawyerId() throws Exception {
        var result = mockMvc.perform(get("/api/lawyers")
                .header("Authorization", bearerToken()))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("content").get(0).get("id").asLong();
    }

    private Long createCase() throws Exception {
        Long lawyerId = getFirstLawyerId();

        Map<String, Object> body = new HashMap<>();
        body.put("caseTypeCode", "CIVIL");
        body.put("tribunalCode", "TR_PIN_1");
        body.put("lawyerIds", Set.of(lawyerId));
        body.put("registrationDate", "2025-01-01");
        body.put("caseDescription", "Test case for time entry IT");

        var caseResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        return objectMapper.readTree(caseResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void listTimeEntries_ShouldReturn200_ForExistingCase() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(get("/api/cases/" + caseId + "/time-entries")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getTimeEntrySummary_ShouldReturn200() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(get("/api/cases/" + caseId + "/time-entries/summary")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalHours").exists());
    }

    @Test
    void createTimeEntry_ShouldReturn201() throws Exception {
        Long caseId = createCase();
        Long lawyerId = getFirstLawyerId();

        Map<String, Object> body = Map.of(
            "lawyerId", lawyerId,
            "entryDate", "2025-05-01",
            "hours", new BigDecimal("1.5"),
            "hourlyRate", new BigDecimal("500.00"),
            "description", "Research and document review",
            "billable", true
        );

        mockMvc.perform(post("/api/cases/" + caseId + "/time-entries")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").value("Research and document review"))
            .andExpect(jsonPath("$.hours").value(1.5));
    }

    @Test
    void updateTimeEntry_ShouldReturn200() throws Exception {
        Long caseId = createCase();
        Long lawyerId = getFirstLawyerId();

        Map<String, Object> createBody = Map.of(
            "lawyerId", lawyerId,
            "entryDate", "2025-05-01",
            "hours", new BigDecimal("1.0"),
            "hourlyRate", new BigDecimal("400.00"),
            "description", "Initial entry",
            "billable", true
        );

        var createResult = mockMvc.perform(post("/api/cases/" + caseId + "/time-entries")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> updateBody = Map.of(
            "lawyerId", lawyerId,
            "entryDate", "2025-05-01",
            "hours", new BigDecimal("2.0"),
            "hourlyRate", new BigDecimal("400.00"),
            "description", "Updated entry",
            "billable", true
        );

        mockMvc.perform(put("/api/time-entries/" + entryId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hours").value(2.0));
    }

    @Test
    void deleteTimeEntry_ShouldReturn204_WhenExists() throws Exception {
        Long caseId = createCase();
        Long lawyerId = getFirstLawyerId();

        Map<String, Object> body = Map.of(
            "lawyerId", lawyerId,
            "entryDate", "2025-05-02",
            "hours", new BigDecimal("0.5"),
            "hourlyRate", new BigDecimal("300.00"),
            "description", "Entry to delete",
            "billable", false
        );

        var createResult = mockMvc.perform(post("/api/cases/" + caseId + "/time-entries")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long entryId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/time-entries/" + entryId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void listTimeEntries_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/cases/1/time-entries"))
            .andExpect(status().isUnauthorized());
    }
}
