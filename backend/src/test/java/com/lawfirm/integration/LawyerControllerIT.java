package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LawyerControllerIT extends BaseIntegrationTest {

    @Test
    void listLawyers_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/lawyers")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk());
    }

    @Test
    void createLawyer_ShouldReturn201_WhenValid() throws Exception {
        Map<String, Object> body = Map.of(
            "firstName", "Youssef",
            "lastName", "Mansouri_" + System.currentTimeMillis(),
            "email", "ymansouri_" + System.currentTimeMillis() + "@law.ma",
            "phone", "0600000000",
            "taxId", "TAX-IT-" + System.currentTimeMillis(),
            "barNumber", "BAR-IT-" + System.currentTimeMillis(),
            "specialization", "Civil Law"
        );

        mockMvc.perform(post("/api/lawyers")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("Youssef"));
    }

    @Test
    void getLawyerById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/lawyers/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void createLawyer_ShouldReturn409_WhenDuplicateTaxId() throws Exception {
        String taxId = "TAX-DUPE-" + System.currentTimeMillis();

        Map<String, Object> body1 = Map.of(
            "firstName", "Lawyer", "lastName", "One",
            "email", "l1_" + System.currentTimeMillis() + "@test.com",
            "taxId", taxId
        );
        mockMvc.perform(post("/api/lawyers")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body1)))
            .andExpect(status().isCreated());

        Map<String, Object> body2 = Map.of(
            "firstName", "Lawyer", "lastName", "Two",
            "email", "l2_" + System.currentTimeMillis() + "@test.com",
            "taxId", taxId
        );
        mockMvc.perform(post("/api/lawyers")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body2)))
            .andExpect(status().isConflict());
    }
}
