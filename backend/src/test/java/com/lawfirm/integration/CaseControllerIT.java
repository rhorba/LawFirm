package com.lawfirm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CaseControllerIT extends BaseIntegrationTest {

    private Long getFirstLawyerId() throws Exception {
        var result = mockMvc.perform(get("/api/lawyers")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString());
        return page.get("content").get(0).get("id").asLong();
    }

    private Map<String, Object> buildCreateCaseBody(Long lawyerId) {
        Map<String, Object> body = new HashMap<>();
        body.put("caseTypeCode", "CIVIL");
        body.put("tribunalCode", "TR_PIN_1");
        body.put("lawyerIds", Set.of(lawyerId));
        body.put("registrationDate", LocalDate.now().toString());
        body.put("caseDescription", "Integration test case description");
        body.put("priority", "NORMAL");
        return body;
    }

    @Test
    void searchCases_ShouldReturn200_WithPagedResult() throws Exception {
        mockMvc.perform(get("/api/cases")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchCases_WithFilters_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/cases")
                .param("caseTypeCode", "CIVIL")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchCases_WithSortAndSearch_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/cases")
                .param("search", "test")
                .param("sortBy", "priority")
                .param("sortDirection", "ASC")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void createCase_ShouldReturn201_WithCaseResponse() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> body = buildCreateCaseBody(lawyerId);

        mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.fullCaseNumber").isNotEmpty())
            .andExpect(jsonPath("$.caseDescription").value("Integration test case description"));
    }

    @Test
    void createCase_ShouldReturn400_WhenMissingRequiredFields() throws Exception {
        Map<String, Object> body = Map.of("caseTypeCode", "CIVIL");

        mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void createCase_ShouldReturn404_WhenCaseTypeNotFound() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> body = new HashMap<>();
        body.put("caseTypeCode", "NONEXISTENT");
        body.put("tribunalCode", "TR_PIN_1");
        body.put("lawyerIds", Set.of(lawyerId));
        body.put("caseDescription", "Test");

        mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound());
    }

    @Test
    void getCaseById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/cases/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getCaseById_ShouldReturn200_WhenExists() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> body = buildCreateCaseBody(lawyerId);

        var createResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long caseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/cases/" + caseId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(caseId));
    }

    @Test
    void updateCase_ShouldReturn200_WhenExists() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> createBody = buildCreateCaseBody(lawyerId);

        var createResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long caseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("lawyerIds", Set.of(lawyerId));
        updateBody.put("caseDescription", "Updated description");
        updateBody.put("priority", "HIGH");

        mockMvc.perform(put("/api/cases/" + caseId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseDescription").value("Updated description"));
    }

    @Test
    void changeStatus_ShouldReturn200_WhenValidTransition() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> createBody = buildCreateCaseBody(lawyerId);

        var createResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long caseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> statusBody = Map.of("statusCode", "OPEN", "reason", "Moving to open status");

        mockMvc.perform(patch("/api/cases/" + caseId + "/status")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusBody)))
            .andExpect(status().isOk());
    }

    @Test
    void getCaseChildren_ShouldReturn200_WithEmptyList() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> createBody = buildCreateCaseBody(lawyerId);

        var createResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long caseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/cases/" + caseId + "/children")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCaseHistory_ShouldReturn200_WithAuditList() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> createBody = buildCreateCaseBody(lawyerId);

        var createResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long caseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/cases/" + caseId + "/history")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteCase_ShouldReturn204_WhenExists() throws Exception {
        Long lawyerId = getFirstLawyerId();
        Map<String, Object> createBody = buildCreateCaseBody(lawyerId);

        var createResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long caseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/cases/" + caseId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteCase_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(delete("/api/cases/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void searchCases_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/cases"))
            .andExpect(status().isUnauthorized());
    }
}
