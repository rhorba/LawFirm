package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupControllerIT extends BaseIntegrationTest {

    @Test
    void getAllGroups_ShouldReturn200_WithList() throws Exception {
        mockMvc.perform(get("/api/groups")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createGroup_ShouldReturn201() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "name", "TestGroup_" + ts,
            "description", "Integration test group",
            "roleIds", List.of()
        );

        mockMvc.perform(post("/api/groups")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("TestGroup_" + ts));
    }

    @Test
    void createGroup_ShouldReturn409_WhenDuplicateName() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "name", "DupGroup_" + ts,
            "description", "First group"
        );

        mockMvc.perform(post("/api/groups")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/groups")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict());
    }

    @Test
    void getGroupById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/groups/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getGroupById_ShouldReturn200_WhenExists() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> createBody = Map.of(
            "name", "GetGroup_" + ts,
            "description", "Test group for get"
        );

        var createResult = mockMvc.perform(post("/api/groups")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long groupId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/groups/" + groupId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(groupId));
    }

    @Test
    void updateGroup_ShouldReturn200_WhenExists() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> createBody = Map.of(
            "name", "UpdGroup_" + ts,
            "description", "Original description"
        );

        var createResult = mockMvc.perform(post("/api/groups")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBody)))
            .andExpect(status().isCreated())
            .andReturn();

        Long groupId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Map<String, Object> updateBody = Map.of(
            "name", "UpdGroup_" + ts,
            "description", "Updated description"
        );

        mockMvc.perform(put("/api/groups/" + groupId)
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void deleteGroup_ShouldReturn204_WhenGroupHasNoUsers() throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> body = Map.of(
            "name", "DelGroup_" + ts,
            "description", "Group to delete"
        );

        var createResult = mockMvc.perform(post("/api/groups")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();

        Long groupId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/groups/" + groupId)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void getAllGroups_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/groups"))
            .andExpect(status().isUnauthorized());
    }
}
