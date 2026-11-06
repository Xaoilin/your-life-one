package com.yourlifeone.prayer.api.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An unreachable database or RabbitMQ never stops the service: health stays UP and data requests
 * fail with a retryable 503.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.port=1",
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/postgres",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.datasource.hikari.connection-timeout=250"
})
@AutoConfigureMockMvc
class PrayerWithoutDatabaseTest {
    private static final String YOUR_LIFE_ONE = "https://your-life-one.example";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void livenessIsUpForTheYourLifeOneOrigin() throws Exception {
        mockMvc.perform(get("/api/prayer/health").header("Origin", YOUR_LIFE_ONE))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", YOUR_LIFE_ONE))
                .andExpect(content().json("{\"status\":\"UP\",\"service\":\"prayer-service\"}"));
    }

    @Test
    void rejectsOtherBrowserOrigins() throws Exception {
        mockMvc.perform(get("/api/prayer/health").header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void reportsTheDatabaseDownAndLogsWhy() throws Exception {
        var logger = (Logger) LoggerFactory.getLogger(PrayerDatabaseHealthController.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        var previousLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        try {
            mockMvc.perform(get("/api/prayer/health/database"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json("{\"status\":\"DOWN\",\"service\":\"prayer-service\",\"database\":\"DOWN\"}"));

            assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage()
                    .contains("exceptionType=DatabaseHealthCheckException")));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
        }
    }

    @Test
    void failsDataRequestsWithABoundedError() throws Exception {
        mockMvc.perform(get("/api/prayer/v1/preferences")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"code\":\"database_unavailable\",\"message\":\"The database is unavailable.\"}"));
    }

    @Test
    void failsWritesWithARetryableErrorWhileTheDatabaseAndRabbitMqAreUnreachable() throws Exception {
        var body = "{\"date\":\"2026-09-25\",\"prayer\":\"Dhuhr\",\"status\":\"on_time\"}";
        mockMvc.perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void requiresASignedInUserForData() throws Exception {
        mockMvc.perform(get("/api/prayer/v1/preferences")).andExpect(status().isUnauthorized());
    }
}
