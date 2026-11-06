package com.yourlifeone.calendar.api.controller;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** An unreachable database never stops the service: health stays UP and data requests fail with 503. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/postgres",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.datasource.hikari.connection-timeout=250"
})
@AutoConfigureMockMvc
class CalendarWithoutDatabaseTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportsTheDatabaseAsUnavailable() throws Exception {
        mockMvc.perform(get("/api/calendar/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/calendar/health/database"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"DOWN\",\"service\":\"calendar-service\",\"database\":\"DOWN\"}"));
        mockMvc.perform(get("/api/calendar/v1/calendar?from=2026-09-01&to=2026-09-30")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"code\":\"database_unavailable\",\"message\":\"The database is unavailable.\"}"));
    }
}
