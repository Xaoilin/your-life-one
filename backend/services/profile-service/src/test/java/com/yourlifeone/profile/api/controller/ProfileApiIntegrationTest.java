package com.yourlifeone.profile.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.profile.application.service.ViewPreferencesService;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The full request path against a real PostgreSQL. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ProfileApiIntegrationTest {
    private static final String SETTINGS = "/api/profile/v1/settings";
    private static final String PREFERENCES = "/api/profile/v1/preferences";
    private static final String INTEGRATIONS = "/api/profile/v1/integrations";
    private static final String EVENTS = "/api/profile/v1/operational-events";
    private static final String PREFERENCES_BODY = """
            {"theme":"dark","dataRetentionDays":30,"telemetry":true,"defaultCalendarTab":"agenda",
             "goalTags":["Health","Work"]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ViewPreferencesService viewPreferences;

    private ResultActions perform(MockHttpServletRequestBuilder request, UUID as) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(as.toString()))));
    }

    private ResultActions save(UUID as, String body) throws Exception {
        return perform(put(SETTINGS).contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    @Test
    void healthIsPublicAndSettingsNeedASignedInUser() throws Exception {
        mockMvc.perform(get("/api/profile/health")).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/profile/health/database")).andExpect(jsonPath("$.database").value("UP"));
        mockMvc.perform(get(SETTINGS)).andExpect(status().isUnauthorized());
    }

    @Test
    void savesAndReadsBackEachUsersOwnSettings() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();

        perform(get(SETTINGS), user).andExpect(jsonPath("$.city").value("Bedford"));
        save(user, "{\"city\":\"London\",\"country\":\"United Kingdom\",\"timeZone\":\"Europe/London\"}")
                .andExpect(status().isOk());

        perform(get(SETTINGS), user)
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.timeZone").value("Europe/London"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        perform(get(SETTINGS), other).andExpect(jsonPath("$.city").value("Bedford"));
    }

    @Test
    void rejectsInvalidSettingsWithABoundedError() throws Exception {
        var user = UUID.randomUUID();

        save(user, "{\"city\":\"London\",\"country\":\"UK\",\"timeZone\":\"+01:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_time_zone"));
        save(user, "{\"city\":\" \",\"country\":\"UK\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        save(user, "{").andExpect(status().isBadRequest());
    }

    @Test
    void allowsBrowserWritesOnlyFromTheYourLifeOneOrigin() throws Exception {
        mockMvc.perform(options(SETTINGS)
                        .header(HttpHeaders.ORIGIN, "https://your-life-one.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://your-life-one.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("idempotency-key")));
        mockMvc.perform(options(SETTINGS)
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void savingTwiceWithOneIdempotencyKeySavesOnceAndReplaysTheResponse() throws Exception {
        var user = UUID.randomUUID();
        var body = "{\"city\":\"Leeds\",\"country\":\"United Kingdom\"}";

        var first = perform(put(SETTINGS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(IdempotencyKeys.HEADER, "settings-1"), user)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(IdempotencyKeys.REPLAYED_HEADER))
                .andReturn().getResponse().getContentAsString();
        var replay = perform(put(SETTINGS).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(IdempotencyKeys.HEADER, "settings-1"), user)
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();

        // The replay carries the first save's timestamp: nothing was written again.
        assertEquals(first, replay);
        perform(put(SETTINGS).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Leeds", "York")).header(IdempotencyKeys.HEADER, "settings-1"), user)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
        perform(get(SETTINGS), user).andExpect(jsonPath("$.city").value("Leeds"));
    }

    @Test
    void savesAndReadsBackAppPreferencesIncludingReorderedGoalTags() throws Exception {
        var user = UUID.randomUUID();

        perform(get(PREFERENCES), user)
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.dataRetentionDays").value(90))
                .andExpect(jsonPath("$.goalTags").isEmpty());
        perform(put(PREFERENCES).contentType(MediaType.APPLICATION_JSON).content(PREFERENCES_BODY), user)
                .andExpect(status().isOk());
        // Swapping two tags moves each name to the other's position within one save.
        perform(put(PREFERENCES).contentType(MediaType.APPLICATION_JSON)
                .content(PREFERENCES_BODY.replace("[\"Health\",\"Work\"]", "[\"Work\",\"Health\"]")), user)
                .andExpect(status().isOk());

        perform(get(PREFERENCES), user)
                .andExpect(jsonPath("$.telemetry").value(true))
                .andExpect(jsonPath("$.defaultCalendarTab").value("agenda"))
                .andExpect(jsonPath("$.goalTags[0]").value("Work"))
                .andExpect(jsonPath("$.goalTags[1]").value("Health"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        perform(put(PREFERENCES).contentType(MediaType.APPLICATION_JSON)
                .content(PREFERENCES_BODY.replace("\"dark\"", "\"neon\"")), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_theme"));
    }

    @Test
    void savesIntegrationRecordsOnlyForOfferedProviders() throws Exception {
        var user = UUID.randomUUID();

        perform(get(INTEGRATIONS), user).andExpect(jsonPath("$.integrations").isEmpty());
        perform(put(INTEGRATIONS + "/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"connected\",\"configuredAt\":\"2026-09-26T10:00:00Z\"}"), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("google"));
        perform(put(INTEGRATIONS + "/github").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"connected\"}"), user)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("unknown_integration"));

        perform(get(INTEGRATIONS), user)
                .andExpect(jsonPath("$.integrations.length()").value(1))
                .andExpect(jsonPath("$.integrations[0].status").value("connected"))
                .andExpect(jsonPath("$.integrations[0].configuredAt").value("2026-09-26T10:00:00Z"));
    }

    @Test
    void concurrentDuplicateSubmissionsWriteOnce() throws Exception {
        var user = UUID.randomUUID();
        var sameKey = concurrently(8, attempt -> perform(put(PREFERENCES).contentType(MediaType.APPLICATION_JSON)
                .content(PREFERENCES_BODY).header(IdempotencyKeys.HEADER, "prefs-save-1"), user));
        var differentKeys = concurrently(8, attempt -> perform(put(INTEGRATIONS + "/google")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"connected\"}")
                .header(IdempotencyKeys.HEADER, "connect-" + attempt), user));

        for (var response : sameKey) assertEquals(200, response.getResponse().getStatus());
        for (var response : differentKeys) assertEquals(200, response.getResponse().getStatus());
        // One key is one write, however many times it arrives at once: every reply is the first save.
        assertEquals(1, sameKey.stream().map(this::body).distinct().count());
        var jdbc = JdbcClient.create(dataSource);
        assertEquals(1, jdbc.sql("SELECT count(*) FROM profile.processed_command WHERE user_id = ? AND command_type = ?")
                .params(user, "profile.update-preferences").query(Integer.class).single());
        assertEquals(1, jdbc.sql("SELECT count(*) FROM profile.integration WHERE user_id = ?").param(user)
                .query(Integer.class).single());
    }

    @Test
    void recordsOperationalEventsAndRefusesUnknownValues() throws Exception {
        var user = UUID.randomUUID();
        var event = """
                {"id":"%s","correlationId":"%s","occurredAt":"%s","release":"0.2.199","domain":"database",
                 "operation":"read","outcome":"failed","reason":"timeout","attempt":1,"durationMs":1200,
                 "recoveryMs":null,"freshness":"stale"}""".formatted(UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS));

        perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON).content("{\"events\":[" + event + "]}"), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.schemaVersion").value(1));
        perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"events\":[" + event.replace("\"database\"", "\"secret-note\"") + "]}"), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
        perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON).content("{\"events\":[]}"), user)
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON).content("{\"events\":[" + event + "]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aUserOverTheRateLimitGets429WithRetryAfterWhileOthersContinue() throws Exception {
        var busy = UUID.randomUUID();
        var status = 200;
        for (int request = 0; request < 200 && status != 429; request++) {
            status = mockMvc.perform(get(SETTINGS).header(HttpHeaders.ORIGIN, "https://your-life-one.example")
                            .with(jwt().jwt(token -> token.subject(busy.toString()))))
                    .andReturn().getResponse().getStatus();
        }
        assertEquals(429, status);

        mockMvc.perform(get(SETTINGS).header(HttpHeaders.ORIGIN, "https://your-life-one.example")
                        .with(jwt().jwt(token -> token.subject(busy.toString()))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Retry-After")))
                .andExpect(jsonPath("$.code").value("rate_limited"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        perform(get(SETTINGS), UUID.randomUUID()).andExpect(status().isOk());
        mockMvc.perform(get("/api/profile/health")).andExpect(status().isOk());
    }

    private String body(MvcResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<MvcResult> concurrently(int count, Attempt attempt)
            throws Exception {
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<MvcResult>>();
        try (var threads = Executors.newFixedThreadPool(count)) {
            for (int index = 0; index < count; index++) {
                var number = index;
                Callable<MvcResult> call = () -> {
                    start.await();
                    return attempt.run(number).andReturn();
                };
                futures.add(threads.submit(call));
            }
            start.countDown();
            var results = new ArrayList<MvcResult>();
            for (var future : futures) results.add(future.get());
            return results;
        }
    }

    @FunctionalInterface
    private interface Attempt {
        ResultActions run(int number) throws Exception;
    }

    private static final String VIEW_PREFERENCES = "/api/profile/v1/view-preferences";

    private ResultActions choose(UUID user, String key, String value, String writeKey) throws Exception {
        return perform(put(VIEW_PREFERENCES + "/" + key).contentType(MediaType.APPLICATION_JSON)
                .header(IdempotencyKeys.HEADER, writeKey).content("{\"value\":\"" + value + "\"}"), user);
    }

    @Test
    void viewChoicesAreAccountOwnedPerKeyAndReadsNeverWriteDefaults() throws Exception {
        var user = UUID.randomUUID();
        var other = UUID.randomUUID();
        perform(get(VIEW_PREFERENCES), user).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.values").isEmpty());
        var jdbc = JdbcClient.create(dataSource);
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM profile.view_preferences WHERE user_id = :user")
                .param("user", user).query(Long.class).single());
        choose(user, "prayerChallenge.period", "day", "view-period").andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("prayerChallenge.period"))
                .andExpect(jsonPath("$.value").value("day")).andExpect(jsonPath("$.updatedAt").isNotEmpty());
        choose(user, "dashboard.learnIndividualSteps", "true", "view-steps").andExpect(status().isOk());
        perform(get(VIEW_PREFERENCES), user).andExpect(jsonPath("$.values['prayerChallenge.period']").value("day"))
                .andExpect(jsonPath("$.values['dashboard.learnIndividualSteps']").value("true"));
        perform(get(VIEW_PREFERENCES), other).andExpect(jsonPath("$.values").isEmpty());
        assertEquals(Boolean.TRUE, jdbc.sql("SELECT relrowsecurity FROM pg_class WHERE oid = 'profile.view_preferences'::regclass")
                .query(Boolean.class).single());
    }

    @Test
    void viewChoiceRetriesAreIdempotentAndRejectAReusedKeyWithDifferentIntent() throws Exception {
        var user = UUID.randomUUID();
        var first = choose(user, "prayerChallenge.period", "week", "stable-choice")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var retry = choose(user, "prayerChallenge.period", "week", "stable-choice")
                .andExpect(status().isOk()).andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(first, retry);
        choose(user, "prayerChallenge.period", "month", "stable-choice")
                .andExpect(status().isUnprocessableContent());
        perform(get(VIEW_PREFERENCES), user).andExpect(jsonPath("$.values['prayerChallenge.period']").value("week"));
    }

    @Test
    void viewChoicesDenyAnonymousAgentsUnknownKeysAndInvalidValues() throws Exception {
        var user = UUID.randomUUID();
        mockMvc.perform(get(VIEW_PREFERENCES)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(VIEW_PREFERENCES + "/prayerChallenge.period").contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"day\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(VIEW_PREFERENCES).with(jwt().jwt(token -> token.subject(user.toString())
                .claim("client_id", "another-domain-agent")))).andExpect(status().isForbidden());
        mockMvc.perform(put(VIEW_PREFERENCES + "/prayerChallenge.period").contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"day\"}").with(jwt().jwt(token -> token.subject(user.toString())
                        .claim("client_id", "another-domain-agent")))).andExpect(status().isForbidden());
        choose(user, "not.allowed", "day", "unknown-view-key").andExpect(status().isBadRequest());
        choose(user, "prayerChallenge.period", "year", "unknown-view-value").andExpect(status().isBadRequest());
        perform(put(VIEW_PREFERENCES + "/prayerChallenge.period").contentType(MediaType.APPLICATION_JSON).content("{}"), user)
                .andExpect(status().isBadRequest());
        perform(get(VIEW_PREFERENCES), user).andExpect(jsonPath("$.values").isEmpty());
    }

    @Test
    void simultaneousFirstChoicesDoNotClobberAnotherKeyOrFailOnTheSameKey() throws Exception {
        var user = UUID.randomUUID();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(3)) {
            List<Callable<Void>> writes = List.of(
                    () -> { start.await(); viewPreferences.save(user, "prayerChallenge.period", "day"); return null; },
                    () -> { start.await(); viewPreferences.save(user, "prayerChallenge.period", "week"); return null; },
                    () -> { start.await(); viewPreferences.save(user, "dashboard.learnIndividualSteps", "true"); return null; });
            var futures = writes.stream().map(pool::submit).toList();
            start.countDown();
            for (var future : futures) future.get();
        }
        var values = viewPreferences.values(user);
        assertThat(values).hasSize(2);
        assertThat(values.get("prayerChallenge.period")).isIn("day", "week");
        assertEquals("true", values.get("dashboard.learnIndividualSteps"));
    }
}
