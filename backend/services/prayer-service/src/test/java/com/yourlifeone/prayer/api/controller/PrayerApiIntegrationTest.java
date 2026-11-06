package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.Fixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The full request path against a real PostgreSQL. The prayer-times provider and the clock are
 * mocked. London on 25 Sep 2026 (BST): Fajr 04:12Z, Sunrise 05:51Z, Dhuhr 11:52Z, Asr 15:08Z,
 * Maghrib 18:12Z, Isha 19:18Z, Midnight 23:52Z.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerApiIntegrationTest {
    private static final String LONDON = "?city=London&country=United Kingdom";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ScheduleSource scheduleSource;

    @MockitoBean
    private Clock clock;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void noonOnTheTwentyFifthInLondon() {
        setTime("2026-09-25T12:00:00Z");
        when(scheduleSource.fetch(anyString(), anyString(), any())).thenAnswer(call -> Fixtures
                .london(call.getArgument(2, LocalDate.class)).toBuilder()
                .id(null)
                .city(call.getArgument(0))
                .country(call.getArgument(1))
                .build());
    }

    private void setTime(String instant) {
        when(clock.instant()).thenReturn(Instant.parse(instant));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, UUID as) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(as.toString()))));
    }

    private ResultActions dashboard(UUID as) throws Exception {
        return perform(get("/api/prayer/v1/dashboard" + LONDON), as);
    }

    private ResultActions record(UUID as, String date, String prayer, String status) throws Exception {
        var body = "{\"date\":\"%s\",\"prayer\":\"%s\",\"status\":\"%s\",\"source\":\"dashboard\"}"
                .formatted(date, prayer, status);
        return perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    private ResultActions correct(UUID as, String id, String status) throws Exception {
        return perform(patch("/api/prayer/v1/outcomes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"), as);
    }

    private static JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void dashboardServesTheLandingPageAndCachesTheTimetable() throws Exception {
        perform(get("/api/prayer/v1/dashboard?city=Cachetown&country=United Kingdom"), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-09-25"))
                .andExpect(jsonPath("$.schedule.timezone").value("Europe/London"))
                .andExpect(jsonPath("$.schedule.hijriDate").value("14 Rabīʿ al-thānī 1448"))
                .andExpect(jsonPath("$.schedule.times.length()").value(8))
                .andExpect(jsonPath("$.schedule.times[0].nameArabic").value("الفجر"))
                .andExpect(jsonPath("$.schedule.windows[1].prayer").value("Dhuhr"))
                .andExpect(jsonPath("$.schedule.windows[1].startsAt").value("2026-09-25T11:52:00Z"))
                .andExpect(jsonPath("$.schedule.windows[1].deadlineName").value("Asr"))
                .andExpect(jsonPath("$.schedule.windows[4].deadlineAt").value("2026-09-25T23:52:00Z"))
                .andExpect(jsonPath("$.preferences.reminderMinutes").value(15))
                // Tracking started at noon UTC: Fajr had already closed, Dhuhr is open.
                .andExpect(jsonPath("$.tracking.activationDate").value("2026-09-25"))
                .andExpect(jsonPath("$.recentDays.length()").value(7))
                .andExpect(jsonPath("$.recentDays[6].prayers[0].status").value("not_tracked"))
                .andExpect(jsonPath("$.recentDays[6].prayers[1].status").value("pending"))
                .andExpect(jsonPath("$.monthStats.overall.pending").value(4))
                .andExpect(jsonPath("$.monthStats.perPrayer.Dhuhr.pending").value(1));

        perform(get("/api/prayer/v1/dashboard?city=cachetown&country=united kingdom"), user).andExpect(status().isOk());
        verify(scheduleSource, times(1)).fetch(eq("Cachetown"), anyString(), any());
    }

    @Test
    void recordsCorrectsAndUndoesAPrayerRewardingItOnce() throws Exception {
        dashboard(user);

        var created = json(record(user, "2026-09-25", "Dhuhr", "on_time")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstReward").value(true))
                .andExpect(jsonPath("$.outcome.deadlineAt").value("2026-09-25T15:08:00Z"))
                .andReturn());
        var id = created.path("outcome").path("id").asString();

        record(user, "2026-09-25", "Dhuhr", "late")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("outcome_exists"));

        correct(user, id, "late")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome.status").value("late"))
                .andExpect(jsonPath("$.outcome.rewarded").value(true))
                .andExpect(jsonPath("$.firstReward").value(false));

        dashboard(user)
                .andExpect(jsonPath("$.todayOutcomes[0].id").value(id))
                .andExpect(jsonPath("$.recentDays[6].prayers[1].status").value("late"));

        perform(delete("/api/prayer/v1/outcomes/" + id), user).andExpect(status().isNoContent());
        perform(delete("/api/prayer/v1/outcomes/" + id), user).andExpect(status().isNotFound());
        record(user, "2026-09-25", "Dhuhr", "on_time").andExpect(jsonPath("$.firstReward").value(true));
    }

    @Test
    void enforcesPrayerTimeRules() throws Exception {
        dashboard(user);

        record(user, "2026-09-25", "Isha", "on_time").andExpect(jsonPath("$.code").value("prayer_not_started"));
        record(user, "2026-09-25", "Dhuhr", "missed").andExpect(jsonPath("$.code").value("prayer_window_open"));
        record(user, "2026-09-27", "Fajr", "on_time").andExpect(jsonPath("$.code").value("prayer_not_started"));
        record(user, "2026-09-25", "Dhuhr", "unclassified")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_status"));
        record(user, "2026-09-25", "Sunrise", "on_time").andExpect(status().isBadRequest());
        perform(get("/api/prayer/v1/outcomes?from=2026-09-30&to=2026-09-01"), user)
                .andExpect(jsonPath("$.code").value("invalid_range"));
    }

    @Test
    void recordsAsrFromDhuhrAndIshaFromMaghribAsIndependentOutcomes() throws Exception {
        setTime("2026-09-25T03:00:00Z");
        dashboard(user);

        setTime("2026-09-25T11:51:59.999Z");
        record(user, "2026-09-25", "Asr", "on_time")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("prayer_not_started"));

        setTime("2026-09-25T11:52:00Z");
        record(user, "2026-09-25", "Asr", "on_time")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome.deadlineAt").value("2026-09-25T18:12:00Z"));

        setTime("2026-09-25T18:11:59.999Z");
        record(user, "2026-09-25", "Isha", "on_time")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("prayer_not_started"));

        setTime("2026-09-25T18:12:00Z");
        record(user, "2026-09-25", "Isha", "on_time")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome.deadlineAt").value("2026-09-25T23:52:00Z"));

        var outcomes = json(perform(get("/api/prayer/v1/outcomes?from=2026-09-25&to=2026-09-25"), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn());
        assertEquals("Asr", outcomes.path(0).path("prayer").asString());
        assertEquals("Isha", outcomes.path(1).path("prayer").asString());
    }

    @Test
    void storesMissesOnceTheirWindowsCloseAndReportsThemInStats() throws Exception {
        dashboard(user);
        record(user, "2026-09-25", "Dhuhr", "on_time");

        setTime("2026-09-26T09:00:00Z");
        dashboard(user).andExpect(jsonPath("$.today").value("2026-09-26"));

        var outcomes = json(perform(get("/api/prayer/v1/outcomes?from=2026-09-25&to=2026-09-25"), user)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].status").value("on_time"))
                .andExpect(jsonPath("$[1].status").value("missed"))
                .andExpect(jsonPath("$[1].source").value("system"))
                .andExpect(jsonPath("$[3].rewarded").value(false))
                .andReturn());

        perform(get("/api/prayer/v1/stats?from=2026-09-25&to=2026-09-25"), user)
                .andExpect(jsonPath("$.overall.onTime").value(1))
                .andExpect(jsonPath("$.overall.missed").value(3))
                .andExpect(jsonPath("$.overall.percentages.onTime").value(25))
                .andExpect(jsonPath("$.perPrayer.Fajr.opportunities").value(0));

        // Praying a missed prayer late is a correction that earns the reward for the first time.
        correct(user, outcomes.path(2).path("id").asString(), "late").andExpect(jsonPath("$.firstReward").value(true));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersOutcomes() throws Exception {
        dashboard(user);
        var id = json(record(user, "2026-09-25", "Dhuhr", "on_time").andReturn()).path("outcome").path("id").asString();
        var other = UUID.randomUUID();

        correct(other, id, "late").andExpect(status().isNotFound());
        perform(delete("/api/prayer/v1/outcomes/" + id), other).andExpect(status().isNotFound());
        perform(get("/api/prayer/v1/outcomes?from=2026-09-25&to=2026-09-25"), other)
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void savesPrayerPreferences() throws Exception {
        var preferences = put("/api/prayer/v1/preferences").contentType(MediaType.APPLICATION_JSON);

        perform(preferences.content("{\"enabled\":true,\"reminderEnabled\":false,\"reminderMinutes\":30}"), user)
                .andExpect(status().isOk());
        perform(get("/api/prayer/v1/preferences"), user).andExpect(jsonPath("$.reminderEnabled").value(false));
        perform(put("/api/prayer/v1/preferences").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"reminderEnabled\":true,\"reminderMinutes\":7}"), user)
                .andExpect(jsonPath("$.code").value("invalid_reminder_minutes"));
        perform(put("/api/prayer/v1/preferences").contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"), user)
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsUnknownAndInvalidLocationsClearly() throws Exception {
        when(scheduleSource.fetch(eq("Atlantis"), anyString(), any()))
                .thenThrow(ApiException.badRequest("location_not_found", "No prayer times were found."));

        perform(get("/api/prayer/v1/schedule?city=Atlantis&country=Sea"), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("location_not_found"));
        perform(get("/api/prayer/v1/dashboard?city= &country=UK"), user)
                .andExpect(jsonPath("$.code").value("invalid_location"));
    }

    @Test
    void allowsBrowserWritesFromTheYourLifeOneOrigin() throws Exception {
        mockMvc.perform(options("/api/prayer/v1/outcomes/" + UUID.randomUUID())
                        .header(HttpHeaders.ORIGIN, "https://your-life-one.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://your-life-one.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
    }
    @Test
    void timingPersistsAndCorrectionsKeepTheOneTimeReward() throws Exception {
        var body = "{\"date\":\"2026-09-25\",\"prayer\":\"Dhuhr\",\"status\":\"on_time\",\"timing\":\"prompt\"}";
        var created = perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "timing-create").content(body), user)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.outcome.timing").value("prompt"))
                .andExpect(jsonPath("$.firstReward").value(true)).andReturn();
        var id = JSON.readTree(created.getResponse().getContentAsString()).path("outcome").path("id").asString();
        perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "timing-create").content(body), user)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.outcome.id").value(id));
        perform(get("/api/prayer/v1/outcomes?from=2026-09-25&to=2026-09-25"), user)
                .andExpect(jsonPath("$[0].timing").value("prompt"));
        perform(patch("/api/prayer/v1/outcomes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"on_time\",\"timing\":\"within_window\"}"), user)
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome.timing").value("within_window"))
                .andExpect(jsonPath("$.outcome.rewarded").value(true)).andExpect(jsonPath("$.firstReward").value(false));
        correct(user, id, "on_time").andExpect(jsonPath("$.outcome.timing").value("within_window"))
                .andExpect(jsonPath("$.firstReward").value(false));
        perform(patch("/api/prayer/v1/outcomes/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"on_time\",\"timing\":\"outside_window\"}"), user)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_timing"));
        perform(get("/api/prayer/v1/competition?from=2026-09-25&to=2026-09-25"), user)
                .andExpect(status().isOk()).andExpect(jsonPath("$.self.pointsEarned").value(8))
                .andExpect(jsonPath("$.self.pointsAvailable").value(12));
        // Simulate a pre-migration row; the migration deliberately leaves its timing null.
        jdbc.update("UPDATE prayer.outcome SET timing = NULL WHERE id = ?", UUID.fromString(id));
        correct(user, id, "on_time").andExpect(jsonPath("$.outcome.timing").doesNotExist());
        perform(get("/api/prayer/v1/competition?from=2026-09-25&to=2026-09-25"), user)
                .andExpect(jsonPath("$.self.pointsEarned").value(12))
                .andExpect(jsonPath("$.self.pointsAvailable").value(12));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE prayer.outcome SET timing = 'outside_window' WHERE id = ?", UUID.fromString(id)));
    }

    @Test
    void allPrayedSettlesTodayOnlyAfterEveryPrayerAndNeverSettlesAFutureWeek() throws Exception {
        setTime("2026-09-25T21:00:00Z");
        for (var prayer : new String[]{"Fajr", "Dhuhr", "Asr", "Maghrib"}) {
            record(user, "2026-09-25", prayer, "on_time").andExpect(status().isCreated());
        }
        var comparison = "/api/prayer/v1/competition?from=2026-09-25&to=2026-09-25";
        perform(get(comparison), user).andExpect(jsonPath("$.self.allPrayed").value(false));
        record(user, "2026-09-25", "Isha", "on_time").andExpect(status().isCreated());
        // All completed records alone cannot manufacture a missing timetable.
        perform(get(comparison), user).andExpect(jsonPath("$.self.allPrayed").value(false));
        dashboard(user).andExpect(status().isOk());
        perform(get(comparison), user).andExpect(jsonPath("$.self.allPrayed").value(true))
                .andExpect(jsonPath("$.self.pointsEarned").value(40))
                .andExpect(jsonPath("$.self.pointsAvailable").value(60));
        perform(get("/api/prayer/v1/competition?from=2026-09-21&to=2026-09-27"), user)
                .andExpect(jsonPath("$.self.allPrayed").value(false));
    }

}
