package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
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
    void listUsers_WithSearch_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("search", "admin")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listUsers_WithRoleFilter_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users")
                .param("role", "ADMIN")
                .param("enabled", "true")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk());
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

    @Test
    void getUserByUsername_ShouldReturn200_WhenExists() throws Exception {
        mockMvc.perform(get("/api/users/username/admin")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void getUserByUsername_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/users/username/nonexistent_xyz999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_ShouldReturn200_WhenValid() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> createBody = Map.of(
            "username", "upd_" + ts,
            "email", "upd_" + ts + "@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        var createResult = mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> updateBody = Map.of(
            "email", "upd_updated_" + ts + "@test.com",
            "enabled", true
        );

        mockMvc.perform(put("/api/users/" + userId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("upd_updated_" + ts + "@test.com"));
    }

    @Test
    void deleteUser_ShouldReturn204_WhenExists() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "username", "del_" + ts,
            "email", "del_" + ts + "@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        var createResult = mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/users/" + userId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void restoreUser_ShouldReturn200_AfterSoftDelete() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "username", "rst_" + ts,
            "email", "rst_" + ts + "@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        var createResult = mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/users/" + userId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/" + userId + "/restore")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk());
    }

    @Test
    void bulkUpdateStatus_ShouldReturn200_WithAffectedCount() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> createBody = Map.of(
            "username", "blk_" + ts,
            "email", "blk_" + ts + "@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        var createResult = mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> bulkBody = Map.of(
            "userIds", List.of(userId),
            "enabled", false
        );

        mockMvc.perform(post("/api/users/bulk/status")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affected").value(1));
    }

    @Test
    void bulkDelete_ShouldReturn200_WithAffectedCount() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> createBody = Map.of(
            "username", "bdl_" + ts,
            "email", "bdl_" + ts + "@test.com",
            "password", "TestPass123!",
            "enabled", true
        );

        var createResult = mockMvc.perform(post("/api/users")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> bulkBody = Map.of("userIds", List.of(userId));

        mockMvc.perform(post("/api/users/bulk/delete")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.affected").value(1));
    }
}
