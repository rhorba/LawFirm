package com.lawfirm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CalendarControllerIT extends BaseIntegrationTest {

    @Test
    void getMonthEvents_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/calendar/month")
                .param("year", "2026")
                .param("month", "5")
                .header("Authorization", bearerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createEvent_ShouldReturn201_WhenValid() throws Exception {
        Map<String, Object> body = Map.of(
            "title", "IT Test Hearing",
            "eventType", "HEARING",
            "startDatetime", "2026-12-01T10:00:00",
            "endDatetime", "2026-12-01T12:00:00",
            "allDay", false
        );

        mockMvc.perform(post("/api/calendar")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("IT Test Hearing"))
            .andExpect(jsonPath("$.eventType").value("HEARING"));
    }

    @Test
    void getEventById_ShouldReturn404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/calendar/999999")
                .header("Authorization", bearerToken()))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteEvent_ShouldReturn204_WhenExists() throws Exception {
        Map<String, Object> body = Map.of(
            "title", "Delete Me",
            "eventType", "REMINDER",
            "startDatetime", "2026-12-15T09:00:00",
            "allDay", false
        );

        var createResult = mockMvc.perform(post("/api/calendar")
                .header("Authorization", bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/calendar/" + id)
                .header("Authorization", bearerToken()))
            .andExpect(status().isNoContent());
    }
}
