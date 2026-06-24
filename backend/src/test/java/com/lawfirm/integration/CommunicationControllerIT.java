package com.lawfirm.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommunicationControllerIT extends BaseIntegrationTest {

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
        body.put("caseDescription", "Test case for communication IT");

        var caseResult = mockMvc.perform(post("/api/cases")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();
        return objectMapper.readTree(caseResult.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void listCommunications_ShouldReturn200_ForExistingCase() throws Exception {
        Long caseId = createCase();

        mockMvc.perform(get("/api/cases/" + caseId + "/communications")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void logCommunication_Note_ShouldReturn201() throws Exception {
        Long caseId = createCase();

        Map<String, Object> body = Map.of(
            "commType", "NOTE",
            "direction", "INTERNAL",
            "subject", "Test note subject",
            "body", "This is a test note body"
        );

        mockMvc.perform(post("/api/cases/" + caseId + "/communications")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.commType").value("NOTE"))
            .andExpect(jsonPath("$.subject").value("Test note subject"));
    }

    @Test
    void logCommunication_Call_ShouldReturn201() throws Exception {
        Long caseId = createCase();

        Map<String, Object> body = Map.of(
            "commType", "CALL",
            "direction", "OUTBOUND",
            "subject", "Client call",
            "body", "Discussed case progress"
        );

        mockMvc.perform(post("/api/cases/" + caseId + "/communications")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.commType").value("CALL"));
    }

    @Test
    void deleteCommunication_ShouldReturn204_WhenExists() throws Exception {
        Long caseId = createCase();

        Map<String, Object> body = Map.of(
            "commType", "NOTE",
            "direction", "INTERNAL",
            "subject", "To delete",
            "body", "Will be deleted"
        );

        var createResult = mockMvc.perform(post("/api/cases/" + caseId + "/communications")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long commId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/communications/" + commId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void listCommunications_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/cases/1/communications"))
            .andExpect(status().isUnauthorized());
    }
}
