package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIT extends BaseIntegrationTest {

    @Test
    void listUsers_ShouldReturn200_WithPagedResults() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void listUsers_ShouldReturn401_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_ShouldReturn201_WhenValid() throws Exception {
        Map<String, Object> body = Map.of(
            "username", "it_user_" + System.currentTimeMillis(),
            "email", "it_" + System.currentTimeMillis() + "@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value(body.get("username")));
    }

    @Test
    void createUser_ShouldReturn409_WhenDuplicateUsername() throws Exception {
        Map<String, Object> body = Map.of(
            "username", "admin",
            "email", "admin_dup@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict());
    }

    @Test
    void getUserById_ShouldReturn200_WhenExists() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getUserById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }
}
