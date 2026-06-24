package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends BaseIntegrationTest {

    @Test
    void login_ShouldReturn200_WithTokens_WhenCredentialsValid() throws Exception {
        Map<String, Object> body = Map.of("username", "admin", "password", "admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void login_ShouldReturn401_WhenPasswordWrong() throws Exception {
        Map<String, Object> body = Map.of("username", "admin", "password", "wrong");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturn400_WhenBodyEmpty() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void refreshToken_ShouldReturn200_WithNewAccessToken() throws Exception {
        Map<String, Object> loginBody = Map.of("username", "admin", "password", "admin123");
        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
            .andReturn();
        String refreshToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
