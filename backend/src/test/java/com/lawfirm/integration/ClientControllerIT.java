package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientControllerIT extends BaseIntegrationTest {

    @Test
    void listClients_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/clients")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk());
    }

    @Test
    void createClient_Individual_ShouldReturn201() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "clientType", "INDIVIDUAL",
            "firstName", "Fatima",
            "lastName", "Zahra_" + ts,
            "cin", "CD" + ts,
            "dateOfBirth", "1985-03-15",
            "email", "fz_" + ts + "@test.ma",
            "phone", "0611223344"
        );

        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("Fatima"))
            .andExpect(jsonPath("$.clientType").value("INDIVIDUAL"));
    }

    @Test
    void createClient_Corporate_ShouldReturn201() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "clientType", "CORPORATE",
            "companyName", "Corp_" + ts,
            "taxNumber", "RC-" + ts,
            "email", "corp_" + ts + "@test.ma"
        );

        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.companyName").value("Corp_" + ts));
    }

    @Test
    void createClient_ShouldReturn400_WhenIndividualMissingName() throws Exception {
        Map<String, Object> body = Map.of(
            "clientType", "INDIVIDUAL",
            "dateOfBirth", "1985-03-15"
        );

        mockMvc.perform(post("/api/clients")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void getClientById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/clients/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void deactivateClient_ShouldReturn204_WhenExists() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "clientType", "INDIVIDUAL",
            "firstName", "Deactivate",
            "lastName", "Me_" + ts,
            "cin", "XY" + ts,
            "dateOfBirth", "1980-01-01"
        );

        var createResult = mockMvc.perform(post("/api/clients")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/clients/" + id)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }
}
