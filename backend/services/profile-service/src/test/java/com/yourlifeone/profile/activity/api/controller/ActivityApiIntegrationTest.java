package com.yourlifeone.profile.activity.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.ratelimit.FixedRateLimitClockConfiguration;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import tools.jackson.databind.json.JsonMapper;

import static com.yourlifeone.profile.activity.api.controller.ActivityEventJson.batch;
import static com.yourlifeone.profile.activity.api.controller.ActivityEventJson.event;
import static com.yourlifeone.profile.activity.api.controller.ActivityEventJson.threeSessions;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Product-usage ingest and insights through the full stack, against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class, FixedRateLimitClockConfiguration.class})
class ActivityApiIntegrationTest {
    private static final String EVENTS = "/api/profile/v1/activity/events";
    private static final String SETTINGS = "/api/profile/v1/settings";
    private static final String INSIGHTS = "/api/profile/v1/activity/insights";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    private final UUID user = UUID.randomUUID();

    private ResultActions perform(MockHttpServletRequestBuilder request, UUID as) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(as.toString()))));
    }

    private ResultActions ingest(UUID as, String body) throws Exception {
        return perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    private long storedEvents(UUID as) {
        return jdbc.sql("SELECT count(*) FROM activity.product_usage_event WHERE user_id = ?").param(as)
                .query(Long.class).single();
    }

    @Test
    void storesEventsAndSummarisesThemOnTheServer() throws Exception {
        ingest(user, batch(threeSessions()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(11))
                .andExpect(jsonPath("$.duplicates").value(0));

        perform(get(INSIGHTS).param("rangeDays", "30"), user)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rangeDays").value(30))
                .andExpect(jsonPath("$.totalEventCount").value(11))
                .andExpect(jsonPath("$.features", contains("application", "calendar", "navigation", "tasks")))
                .andExpect(jsonPath("$.summary.eventCount").value(10))
                .andExpect(jsonPath("$.summary.sessionCount").value(3))
                .andExpect(jsonPath("$.summary.activeSurfaceCount").value(3))
                .andExpect(jsonPath("$.summary.errorCount").value(2))
                .andExpect(jsonPath("$.summary.failureRate").value(67))
                .andExpect(jsonPath("$.coldStart").value(false))
                .andExpect(jsonPath("$.trends[0].key").value("application:session_started"))
                .andExpect(jsonPath("$.trends[1].key").value("navigation:viewed"))
                .andExpect(jsonPath("$.trends[?(@.key == 'tasks:created')].changePercent").value(0))
                .andExpect(jsonPath("$.funnel[*].sessionCount", contains(3, 3, 1)))
                .andExpect(jsonPath("$.funnel[*].conversionPercent", contains(100, 100, 33)))
                .andExpect(jsonPath("$.errors[0].code").value("calendar_render"))
                .andExpect(jsonPath("$.errors[0].surface").value("calendar"))
                .andExpect(jsonPath("$.errors[1].code").value("save_failed"))
                .andExpect(jsonPath("$.errors[1].surface").value("application"))
                .andExpect(jsonPath("$.recommendations").isEmpty());
    }

    @Test
    void filtersNarrowTheWindowAndTheEvents() throws Exception {
        ingest(user, batch(threeSessions())).andExpect(status().isOk());

        perform(get(INSIGHTS).param("kind", "error"), user)
                .andExpect(jsonPath("$.summary.eventCount").value(1))
                .andExpect(jsonPath("$.coldStart").value(true))
                .andExpect(jsonPath("$.funnel").isEmpty());
        perform(get(INSIGHTS).param("surface", "tasks").param("rangeDays", "90"), user)
                .andExpect(jsonPath("$.summary.eventCount").value(2))
                .andExpect(jsonPath("$.totalEventCount").value(11));
        perform(get(INSIGHTS).param("rangeDays", "7").param("feature", "tasks").param("outcome", "all"), user)
                .andExpect(jsonPath("$.summary.eventCount").value(2))
                .andExpect(jsonPath("$.trends[?(@.key == 'tasks:created')].previousCount").value(0));
        perform(get(INSIGHTS).param("rangeDays", "14"), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_range"));
        perform(get(INSIGHTS).param("surface", "nowhere"), user)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_filter"));
    }

    @Test
    void aRepeatedEventIsADuplicateNeverAnError() throws Exception {
        var session = UUID.randomUUID();
        var first = event(session, 1, "session", "application", "session_started");
        var samePlace = event(session, 1, "session", "application", "session_started");

        ingest(user, batch(List.of(first, first))).andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.duplicates").value(1));
        ingest(user, batch(List.of(first, samePlace, event(session, 2, "action", "tasks", "created"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.duplicates").value(2));
        assertEquals(2, storedEvents(user));
    }

    @Test
    void theSameEventSentConcurrentlyIsStoredOnce() throws Exception {
        var body = batch(List.of(event(UUID.randomUUID(), 1, "session", "application", "session_started")));

        var results = concurrently(8, attempt -> perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON)
                .content(body).header(IdempotencyKeys.HEADER, "activity-" + attempt), user));

        var accepted = 0;
        for (var result : results) {
            assertEquals(200, result.getResponse().getStatus());
            accepted += JSON.readTree(result.getResponse().getContentAsString()).path("accepted").asInt();
        }
        assertEquals(1, accepted);
        assertEquals(1, storedEvents(user));
    }

    @Test
    void oneIdempotencyKeyStoresOnceReplaysTheReceiptAndRefusesAnotherBatch() throws Exception {
        var events = batch(List.of(event(UUID.randomUUID(), 1, "session", "application", "session_started")));

        var first = perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON).content(events)
                .header(IdempotencyKeys.HEADER, "activity-batch-1"), user)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(IdempotencyKeys.REPLAYED_HEADER))
                .andReturn().getResponse().getContentAsString();
        var replay = perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON).content(events)
                .header(IdempotencyKeys.HEADER, "activity-batch-1"), user)
                .andExpect(status().isOk())
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();

        // The replay is the first receipt (accepted, not duplicates): nothing ran again.
        assertEquals(first, replay);
        assertEquals("{\"accepted\":1,\"duplicates\":0}", replay);
        perform(post(EVENTS).contentType(MediaType.APPLICATION_JSON)
                .content(batch(List.of(event(UUID.randomUUID(), 2, "action", "tasks", "created"))))
                .header(IdempotencyKeys.HEADER, "activity-batch-1"), user)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
        assertEquals(1, storedEvents(user));
    }

    @Test
    void refusesTheWholeBatchWhenAnyEventCarriesContentOrLeavesTheTaxonomy() throws Exception {
        var session = UUID.randomUUID();
        var good = event(session, 1, "session", "application", "session_started");

        ingest(user, batch(List.of(good, event(session, 2, "action", "tasks", "created")
                .with("metadata", Map.of("note", "private text")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_metadata"));
        ingest(user, batch(List.of(good, event(session, 2, "click", "tasks", "created"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_taxonomy"));
        ingest(user, "{\"events\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_batch_size"));
        ingest(user, "{\"events\":[{\"eventId\":\"not-a-uuid\"}]}").andExpect(status().isBadRequest());
        assertEquals(0, storedEvents(user));
        mockMvc.perform(get(INSIGHTS)).andExpect(status().isUnauthorized());
    }

    /**
     * The bucket is per user across every endpoint. A fixed test clock prevents a real-time refill
     * between the first refused settings read and the insights assertion; unit tests cover refilling.
     */
    @Test
    void aUserOverTheRateLimitGets429() throws Exception {
        var status = 200;
        for (int request = 0; request < 400 && status != 429; request++) {
            status = perform(get(SETTINGS), user).andReturn().getResponse().getStatus();
        }
        assertEquals(429, status);

        perform(get(INSIGHTS), user)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("rate_limited"));
        perform(get(INSIGHTS), UUID.randomUUID()).andExpect(status().isOk());
    }

    private List<MvcResult> concurrently(int count, Attempt attempt) throws Exception {
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
}
