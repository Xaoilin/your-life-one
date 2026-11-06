package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.Fixtures;
import com.yourlifeone.prayer.infrastructure.persistence.repository.PrayerProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The service decides when a deadline reminder is due and sends it once, as a {@code prayer.reminder} event
 * the live-update gateway streams to open tabs. London on 25 Sep 2026: Fajr ends at Sunrise 05:51Z, Asr at
 * Maghrib 18:12Z and Isha at Midnight 23:52Z; reminders are 15 minutes ahead. The scheduler only runs when a test calls it.
 */
@SpringBootTest(properties = {"spring.datasource.hikari.data-source-properties.sslmode=disable",
        "app.prayer.reminder-check-interval=PT24H"})
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerRemindersIntegrationTest {
    private static final ContractFixtures LIVE_CONTRACT = new ContractFixtures("contracts/live-service");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderScheduler reminders;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PrayerProfileRepository profiles;

    @Autowired
    private ReminderEvaluationService evaluation;

    @MockitoBean
    private ScheduleSource scheduleSource;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private ChangePublisher changes;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        jdbc.sql("INSERT INTO auth.users (id) VALUES (?)").param(user).update();
        when(scheduleSource.fetch(anyString(), anyString(), any())).thenAnswer(call -> Fixtures
                .london(call.getArgument(2, LocalDate.class)).toBuilder().id(null)
                .city(call.getArgument(0)).country(call.getArgument(1)).build());
        at("2026-09-25T05:00:00Z");
        perform(get("/api/prayer/v1/dashboard?city=London&country=United Kingdom"));
        clearInvocations(changes);
    }

    private void at(String instant) {
        when(clock.instant()).thenReturn(Instant.parse(instant));
    }

    private void perform(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString())))).andExpect(status().is2xxSuccessful());
    }

    private JsonNode sentReminder() {
        var data = ArgumentCaptor.forClass(JsonNode.class);
        verify(changes).publish(eq(user), eq(ReminderEvents.REMINDER), data.capture());
        return data.getValue();
    }

    @Test
    void aDueReminderIsSentOnceWithTheShapeTheAppReads() {
        at("2026-09-25T05:40:00Z");
        reminders.sendDueReminders();

        var data = sentReminder();
        var event = json.createObjectNode().put("type", ReminderEvents.REMINDER).put("domain", "prayer")
                .put("at", "2026-09-25T05:40:00Z");
        event.set("data", data);
        LIVE_CONTRACT.check("prayer-reminder-event", "GET", "/api/live/v1/events", 200, event.toString());
        assertEquals("Fajr|Sunrise|2026-09-25T05:51:00Z", data.get("prayer").asString()
                + "|" + data.get("deadline").asString() + "|" + data.get("deadlineAt").asString());

        clearInvocations(changes);
        reminders.sendDueReminders();
        verify(changes, never()).publish(eq(user), eq(ReminderEvents.REMINDER), any(JsonNode.class));
    }

    @Test
    void aRecordedPrayerGetsNoReminder() throws Exception {
        at("2026-09-25T15:10:00Z");
        perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-09-25\",\"prayer\":\"Asr\",\"status\":\"on_time\"}"));
        clearInvocations(changes);

        at("2026-09-25T18:00:00Z");
        reminders.sendDueReminders();

        verify(changes, never()).publish(eq(user), eq(ReminderEvents.REMINDER), any(JsonNode.class));
    }

    @Test
    void ishaIsRemindedAfterMidnightLocalTimeOnItsOwnDate() {
        at("2026-09-25T23:40:00Z");
        reminders.sendDueReminders();

        var data = sentReminder();
        assertEquals("2026-09-25|Isha", data.get("date").asString() + "|"
                + data.get("prayer").asString());
    }

    @Test
    void deletingAnAuthIdentityStopsSubsequentSchedulerWorkWithoutDeletingHistory() throws Exception {
        var valid = UUID.randomUUID();
        jdbc.sql("INSERT INTO auth.users (id) VALUES (?)").param(valid).update();
        mockMvc.perform(get("/api/prayer/v1/dashboard?city=London&country=United Kingdom")
                .with(jwt().jwt(token -> token.subject(valid.toString())))).andExpect(status().isOk());
        at("2026-09-25T05:40:00Z");
        reminders.sendDueReminders();
        assertEquals(1, receiptCount(user));

        jdbc.sql("DELETE FROM auth.users WHERE id = ?").param(user).update();
        // Simulate an already selected candidate and a still-valid JWT re-enabling the retained profile.
        jdbc.sql("UPDATE prayer.profile SET enabled = true WHERE user_id = ?").param(user).update();
        clearInvocations(changes);
        at("2026-09-25T11:53:00Z");
        assertEquals(false, profiles.findEligibleUserIds().contains(user));
        assertEquals(0, evaluation.claimDue(user, clock.instant()).size());
        reminders.sendDueReminders();

        verify(changes, never()).publish(eq(user), anyString(), any(JsonNode.class));
        verify(changes, times(2)).publish(eq(valid), eq(ReminderEvents.NOTICE), any(JsonNode.class));
        assertEquals(1, receiptCount(user));
        assertEquals(3, receiptCount(valid));
        assertEquals(1, jdbc.sql("SELECT count(*) FROM prayer.profile WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }

    private int receiptCount(UUID userId) {
        return jdbc.sql("""
                SELECT (SELECT count(*) FROM prayer.reminder WHERE user_id = :userId)
                     + (SELECT count(*) FROM prayer.notice WHERE user_id = :userId)""")
                .param("userId", userId).query(Integer.class).single();
    }
}
