package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerIT extends BaseIntegrationTest {

    @Test
    void getUpcomingTasks_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/tasks/upcoming")
                .param("days", "7")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getTasksByCase_ShouldReturn200_WithEmptyList_WhenCaseNotFound() throws Exception {
        mockMvc.perform(get("/api/cases/999999/tasks")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getTaskById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }
}
