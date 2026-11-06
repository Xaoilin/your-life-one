package com.yourlifeone.live.api.controller;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A change published on RabbitMQ reaches the open streams of its user, and only theirs. The event data keeps
 * the shape of the fixtures in {@code contracts/live-service}, which the React app parses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RabbitTestContainer.class)
class LiveEventsIntegrationTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/live-service");
    private static final String EVENTS = "/api/live/v1/events";
    private static final Duration WAIT = Duration.ofSeconds(10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChangePublisher changes;

    @Autowired
    private JsonMapper json;

    private MockHttpServletResponse open(UUID user) throws Exception {
        var expiresAt = Instant.now().plus(Duration.ofHours(1));
        return mockMvc.perform(get(EVENTS).with(jwt().jwt(token -> token.subject(user.toString()).expiresAt(expiresAt))))
                .andExpect(request().asyncStarted())
                .andReturn().getResponse();
    }

    /** The data line of the first event with that type on the stream, waiting for it to arrive. */
    private static String dataOf(MockHttpServletResponse stream, String type) throws Exception {
        var deadline = Instant.now().plus(WAIT);
        while (Instant.now().isBefore(deadline)) {
            for (var line : stream.getContentAsString().split("\n")) {
                if (line.startsWith("data:") && line.contains("\"type\":\"" + type + "\"")) return line.substring(5);
            }
            Thread.sleep(50);
        }
        throw new AssertionError("No " + type + " event on the stream: " + stream.getContentAsString());
    }

    @Test
    void aUsersChangesReachOnlyTheirOwnStreams() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        var mine = open(user);
        var theirs = open(other);

        changes.publish(user, "lifestyle.save-item");

        var data = dataOf(mine, "lifestyle.save-item");
        assertTrue(mine.getContentAsString().startsWith("event:ready"));
        CONTRACT.check("change-event", "GET", EVENTS, 200, data);
        assertEquals("lifestyle", json.readTree(data).get("domain").asString());
        changes.publish(other, "tasks.save-task");
        dataOf(theirs, "tasks.save-task");
        assertFalse(mine.getContentAsString().contains("tasks.save-task"));
        assertFalse(theirs.getContentAsString().contains("lifestyle.save-item"));
    }

    @Test
    void aPrayerReminderCarriesItsDetails() throws Exception {
        var user = UUID.randomUUID();
        var stream = open(user);

        changes.publish(user, "prayer.reminder", json.readTree("""
                {"date":"2026-09-27","prayer":"Fajr","deadlineAt":"2026-09-27T06:05:00Z","deadline":"Sunrise",
                 "timeZone":"Europe/London","reminderMinutes":15}"""));

        CONTRACT.check("prayer-reminder-event", "GET", EVENTS, 200, dataOf(stream, "prayer.reminder"));
    }

    @Test
    void aPrayerNoticeCarriesItsDetails() throws Exception {
        var user = UUID.randomUUID();
        var stream = open(user);

        changes.publish(user, "prayer.notice", json.readTree("""
                {"key":"momentum:2026-09-27:1790337120000:learn+move","kind":"momentum","date":"2026-09-27",
                 "prayer":"Maghrib","pillars":["learn","move"],"firesAt":"2026-09-27T18:08:00Z",
                 "expiresAt":"2026-09-27T19:14:00Z","deadlineAt":null,"deadline":null,"timeZone":"Europe/London",
                 "reminderMinutes":15,"snoozedUntil":null,"snoozeCount":0}"""));

        CONTRACT.check("prayer-notice-event", "GET", EVENTS, 200, dataOf(stream, "prayer.notice"));
    }

    @Test
    void aStreamNeedsASignedInUser() throws Exception {
        mockMvc.perform(get(EVENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    void tooManyReconnectsAreRateLimited() throws Exception {
        var user = UUID.randomUUID();
        var status = 0;
        String body = null;
        for (int attempt = 0; attempt < 200 && status != 429; attempt++) {
            var result = mockMvc.perform(get(EVENTS).with(jwt().jwt(token -> token.subject(user.toString())))).andReturn();
            status = result.getResponse().getStatus();
            body = result.getResponse().getContentAsString();
        }
        CONTRACT.check("rate-limited", "GET", EVENTS, status, body);
    }
}
