package com.yourlifeone.calendar.api.controller;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.model.ConnectedGoogleAccount;
import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.model.GoogleEvent;
import com.yourlifeone.calendar.application.model.GoogleTokenGrant;
import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.application.port.GoogleOAuthApi;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.metrics.BusinessGaugePublisher;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;
import static org.awaitility.Awaitility.await;
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

/** The full request path against a real PostgreSQL and RabbitMQ; Google's Calendar and OAuth APIs are mocked. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class CalendarApiIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String RANGE = "?from=2026-09-01&to=2026-12-31";
    private static final String EMAIL = "yourlife@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MeterRegistry meters;

    @Autowired
    private BusinessGaugePublisher gauges;

    @MockitoBean
    private GoogleOAuthApi oauth;

    @MockitoBean
    private GoogleCalendarApi google;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void googleAccountWithOneWritableAndOneReadOnlyCalendar() {
        var grant = GoogleTokenGrant.builder().accessToken("google-token").refreshToken("refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(oauth.exchangeCode(eq("code-1"), anyString())).thenReturn(grant);
        when(oauth.account("google-token")).thenReturn(new ConnectedGoogleAccount(EMAIL, "YourLife"));
        when(oauth.refresh("refresh-token")).thenReturn(grant);
        when(google.calendars("google-token")).thenReturn(List.of(
                GoogleCalendar.builder().id(EMAIL).summary("YourLife").primary(true).accessRole("owner")
                        .backgroundColor("#123456").build(),
                GoogleCalendar.builder().id("holidays@group").summary("Holidays").accessRole("reader").build()));
        when(google.events(eq("google-token"), eq(EMAIL), any(), any())).thenReturn(List.of(
                googleEvent("g1", "Dentist", "2026-10-01T09:00:00Z", "2026-10-01T10:00:00Z"),
                googleEvent("g2", "Gym", "2026-10-02T18:00:00Z", "2026-10-02T19:00:00Z")));
        when(google.events(eq("google-token"), eq("holidays@group"), any(), any())).thenReturn(List.of(
                GoogleEvent.builder().id("h1").title("Bank holiday").description("")
                        .timing(EventTiming.parse(true, "2026-12-25", "2026-12-25")).build()));
    }

    private static GoogleEvent googleEvent(String id, String title, String start, String end) {
        return GoogleEvent.builder().id(id).title(title).description("")
                .timing(EventTiming.timed(Instant.parse(start), Instant.parse(end))).build();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString()).tokenValue("user-token"))));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private JsonNode connectGoogle() throws Exception {
        return json(send(post("/api/calendar/v1/accounts/google"),
                "{\"code\":\"code-1\",\"redirectUri\":\"https://your-life-one.example\"}").andExpect(status().isOk()));
    }

    @Test
    void concurrentUnchangedSyncsShareOneProviderPassAndReadNoEventBodies() throws Exception {
        connectGoogle();
        clearInvocations(google);
        var evidence = new CalendarSyncEvidence(dataSource);
        evidence.reset();
        var providerEntered = new CountDownLatch(1);
        var releaseProvider = new CountDownLatch(1);
        var waitingDevice = new AtomicReference<Thread>();
        when(google.events(eq("google-token"), eq(EMAIL), any(), any())).thenAnswer(invocation -> {
            providerEntered.countDown();
            assertTrue(releaseProvider.await(10, TimeUnit.SECONDS));
            return List.of(
                    googleEvent("g1", "Dentist", "2026-10-01T09:00:00Z", "2026-10-01T10:00:00Z"),
                    googleEvent("g2", "Gym", "2026-10-02T18:00:00Z", "2026-10-02T19:00:00Z"));
        });
        try (var devices = Executors.newFixedThreadPool(2)) {
            var first = devices.submit(() -> perform(post("/api/calendar/v1/sync")).andReturn());
            assertTrue(providerEntered.await(10, TimeUnit.SECONDS));
            var second = devices.submit(() -> {
                waitingDevice.set(Thread.currentThread());
                return perform(post("/api/calendar/v1/sync")).andReturn();
            });
            try {
                await().atMost(Duration.ofSeconds(10)).until(() -> waitingDevice.get() != null
                        && Arrays.stream(waitingDevice.get().getStackTrace()).anyMatch(frame ->
                                frame.getClassName().endsWith("CalendarSyncService")
                                        && frame.getMethodName().equals("tryLock")));
            } finally {
                releaseProvider.countDown();
            }
            assertEquals(200, first.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(200, second.get(10, TimeUnit.SECONDS).getResponse().getStatus());
        }
        System.out.println("KAN-330 concurrent-unchanged " + JSON.writeValueAsString(evidence.eventReads()));
        System.out.println("KAN-330 concurrent-business " + JSON.writeValueAsString(evidence.businessReads()));
        verify(google, times(1)).calendars("google-token");
        assertEquals(0, evidence.returnedEventRows(), "unchanged comparison must not transfer event bodies");
    }

    @Test
    void largeUnchangedExplicitRefreshesThenChangedDeletedAndNewEventsConverge() throws Exception {
        var fetched = new ArrayList<GoogleEvent>();
        for (int index = 0; index < 1483; index++) {
            fetched.add(GoogleEvent.builder().id("fixture-" + index).title("Invented event " + index)
                    .description("x".repeat(1024)).location("Invented location")
                    .timing(EventTiming.parse(false, "2026-10-20T10:00:00Z", "2026-10-20T11:00:00Z")).build());
        }
        when(google.events(eq("google-token"), eq(EMAIL), any(), any())).thenReturn(fetched);
        connectGoogle();
        var evidence = new CalendarSyncEvidence(dataSource);
        var fullJsonBytes = evidence.fullEventJsonBytes(user);
        clearInvocations(google);
        evidence.reset();
        perform(post("/api/calendar/v1/sync")).andExpect(status().isOk());
        perform(post("/api/calendar/v1/sync")).andExpect(status().isOk());
        var unchangedRows = evidence.returnedEventRows();
        System.out.println("KAN-330 large-unchanged " + JSON.writeValueAsString(Map.of(
                "fixtureEvents", 1484, "fixtureFullRowJsonBytesProxy", fullJsonBytes,
                "statements", evidence.eventReads(), "businessStatements", evidence.businessReads())));
        // Separate explicit refreshes always call Google, even immediately after a successful pass.
        verify(google, times(2)).calendars("google-token");
        var changed = new ArrayList<>(fetched);
        changed.remove(1);
        changed.set(0, GoogleEvent.builder().id("fixture-0").title("Changed event")
                .description("").location(null).timing(fetched.get(0).timing()).build());
        changed.add(googleEvent("new", "New event", "2026-10-21T10:00:00Z", "2026-10-21T11:00:00Z"));
        when(google.events(eq("google-token"), eq(EMAIL), any(), any())).thenReturn(changed);
        evidence.reset();
        perform(post("/api/calendar/v1/sync")).andExpect(status().isOk());
        var changedRows = evidence.returnedEventRows();
        System.out.println("KAN-330 changed-deleted-inserted " + JSON.writeValueAsString(evidence.eventReads()));
        var result = json(perform(get("/api/calendar/v1/calendar" + RANGE)));
        assertEquals(1484, result.path("events").size());
        var titles = new ArrayList<String>();
        result.path("events").forEach(event -> titles.add(event.path("title").asString()));
        assertTrue(titles.contains("Changed event"));
        assertTrue(titles.contains("New event"));
        assertTrue(!titles.contains("Invented event 1"));
        assertEquals(0, unchangedRows, "independent unchanged refreshes return no event rows");
        assertEquals(4, changedRows, "three difference IDs and one changed body, independent of 1484 stored events");
    }

    private String sourceId(String googleCalendarId) throws Exception {
        for (var source : json(perform(get("/api/calendar/v1/calendar" + RANGE))).path("sources")) {
            if (googleCalendarId.equals(source.path("googleCalendarId").asString())) return source.path("id").asString();
        }
        throw new AssertionError("No source for " + googleCalendarId);
    }

    @Test
    void keepsAWarmConnectionAndReplacesEvictedConnections() throws Exception {
        var pool = assertDoesNotThrow(() -> dataSource.unwrap(HikariDataSource.class));

        assertEquals(1, pool.getMinimumIdle());
        assertEquals(3, pool.getMaximumPoolSize(), "keep the existing bounded calendar pool");
        assertEquals(60_000, pool.getKeepaliveTime());
        assertEquals(1_800_000, pool.getMaxLifetime());
        assertEquals(120_000, pool.getIdleTimeout());
        pool.getHikariPoolMXBean().softEvictConnections();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertTrue(pool.getHikariPoolMXBean().getIdleConnections() >= 1));
        try (var connection = pool.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT 1")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void timesEveryRequestAndDatabaseCallForGrafana() throws Exception {
        perform(get("/api/calendar/v1/calendar" + RANGE)).andExpect(status().isOk());

        var request = meters.find("http.server.requests").tag("uri", "/api/calendar/v1/calendar").timer();
        var repository = meters.find("spring.data.repository.invocations").tag("repository", "AccountRepository").timer();
        assertNotNull(request, "request timer");
        assertNotNull(repository, "database repository timer");
        assertEquals("calendar-service", request.getId().getTag("service"));
        assertEquals(3, request.takeSnapshot().percentileValues().length);
    }

    @Test
    void publishesUpcomingEventsAndBusyHoursPerCalendarForGrafana() throws Exception {
        var name = "Metrics " + UUID.randomUUID();
        send(post("/api/calendar/v1/accounts"), "{\"name\":\"%s\"}".formatted(name)).andExpect(status().isCreated());
        var source = json(perform(get("/api/calendar/v1/calendar" + RANGE))).path("sources").get(0);
        var start = Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MINUTES);
        send(post("/api/calendar/v1/events"), """
                {"sourceId":"%s","title":"Focus","allDay":false,"start":"%s","end":"%s"}"""
                .formatted(source.path("id").asString(), start, start.plus(Duration.ofHours(2))))
                .andExpect(status().isCreated());

        gauges.refresh();

        var calendar = source.path("name").asString();
        assertEquals(1, meters.get("yourlife.calendar.events.next30days").tag("calendar", calendar).gauge().value());
        assertEquals(2, meters.get("yourlife.calendar.busy.hours.next7days").tag("calendar", calendar).gauge().value(), 0.01);
        assertTrue(meters.get("yourlife.writes").tag("command", "calendar.create-event").tag("outcome", "saved")
                .counter().count() >= 1);
    }

    @Test
    void healthIsPublicAndEveryCalendarRouteNeedsASignedInUser() throws Exception {
        mockMvc.perform(get("/api/calendar/health")).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/calendar/health/database")).andExpect(jsonPath("$.database").value("UP"));
        mockMvc.perform(get("/api/calendar/v1/calendar" + RANGE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/calendar/v1/sync")).andExpect(status().isUnauthorized());
    }

    @Test
    void theBrowserMayCallFromTheAppOriginAndReadTheStatusOfARejectedToken() throws Exception {
        mockMvc.perform(options("/api/calendar/v1/events/e1")
                        .header(HttpHeaders.ORIGIN, "https://your-life-one.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://your-life-one.example"));
        // A 401 carries CORS headers, so the app sees "unauthorized" (and refreshes) rather than a network error.
        mockMvc.perform(get("/api/calendar/v1/calendar" + RANGE).header(HttpHeaders.ORIGIN, "https://your-life-one.example"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://your-life-one.example"));
    }

    @Test
    void localAccountsCalendarsAndEventsAreKeptPerUser() throws Exception {
        var account = json(send(post("/api/calendar/v1/accounts"), "{\"name\":\"Home\",\"paletteIndex\":2}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("local"))
                .andExpect(jsonPath("$.isPrimary").value(true)));
        var calendar = json(perform(get("/api/calendar/v1/calendar" + RANGE)));
        assertEquals(1, calendar.path("sources").size());
        var sourceId = calendar.path("sources").get(0).path("id").asString();

        var event = json(send(post("/api/calendar/v1/events"), """
                {"sourceId":"%s","title":"Trip","allDay":true,"start":"2026-10-10","end":"2026-10-12"}"""
                .formatted(sourceId)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.start").value("2026-10-10"))
                .andExpect(jsonPath("$.end").value("2026-10-12")));
        var eventId = event.path("id").asString();

        send(put("/api/calendar/v1/events/" + eventId), """
                {"sourceId":"%s","title":"Call","allDay":false,"start":"2026-10-10T09:00:00+01:00",
                 "end":"2026-10-10T09:30:00+01:00","timeZone":"Europe/London"}""".formatted(sourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value("2026-10-10T08:00:00Z"));
        perform(get("/api/calendar/v1/events?from=2026-10-10&to=2026-10-10")).andExpect(jsonPath("$", hasSize(1)));
        perform(get("/api/calendar/v1/events?from=2026-11-01&to=2026-11-30")).andExpect(jsonPath("$", hasSize(0)));

        send(patch("/api/calendar/v1/sources/" + sourceId), "{\"visible\":false,\"color\":\"#ff0000\"}")
                .andExpect(jsonPath("$.visible").value(false));
        perform(delete("/api/calendar/v1/events/" + eventId)).andExpect(status().isNoContent());
        perform(delete("/api/calendar/v1/accounts/" + account.path("id").asString())).andExpect(status().isNoContent());
        perform(get("/api/calendar/v1/calendar" + RANGE)).andExpect(jsonPath("$.accounts", hasSize(0)));
        verify(google, never()).create(anyString(), anyString(), any());
    }

    @Test
    void aRetriedWriteWithTheSameIdempotencyKeyIsAppliedOnce() throws Exception {
        var body = "{\"name\":\"Work\"}";
        var first = send(post("/api/calendar/v1/accounts").header(IdempotencyKeys.HEADER, "account-1"), body)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var replay = send(post("/api/calendar/v1/accounts").header(IdempotencyKeys.HEADER, "account-1"), body)
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(first, replay);
        perform(get("/api/calendar/v1/calendar" + RANGE)).andExpect(jsonPath("$.accounts", hasSize(1)));
    }

    @Test
    void connectingGoogleMirrorsItsCalendarsAndSyncKeepsThemCurrent() throws Exception {
        connectGoogle();
        perform(get("/api/calendar/v1/calendar" + RANGE))
                .andExpect(jsonPath("$.accounts[0].email").value(EMAIL))
                .andExpect(jsonPath("$.accounts[0].authStatus").value("connected"))
                .andExpect(jsonPath("$.sources", hasSize(2)))
                .andExpect(jsonPath("$.events", hasSize(3)));

        // Google moved the dentist and deleted the gym session.
        when(google.events(eq("google-token"), eq(EMAIL), any(), any())).thenReturn(List.of(
                googleEvent("g1", "Dentist", "2026-10-01T11:00:00Z", "2026-10-01T12:00:00Z")));
        perform(post("/api/calendar/v1/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].status").value("synced"))
                // The dentist, plus the bank holiday on the read-only calendar.
                .andExpect(jsonPath("$.accounts[0].eventCount").value(2));

        perform(get("/api/calendar/v1/events?from=2026-10-01&to=2026-10-02"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].start").value("2026-10-01T11:00:00Z"));
    }

    @Test
    void googleEventsAreWrittenToGoogleFirstAndReadOnlyCalendarsRefuseChanges() throws Exception {
        connectGoogle();
        when(google.create(eq("google-token"), eq(EMAIL), any())).thenReturn(
                googleEvent("g3", "Lunch", "2026-10-05T12:00:00Z", "2026-10-05T13:00:00Z"));

        send(post("/api/calendar/v1/events"), """
                {"sourceId":"%s","title":"Lunch","allDay":false,"start":"2026-10-05T12:00:00Z",
                 "end":"2026-10-05T13:00:00Z","timeZone":"Europe/London"}""".formatted(sourceId(EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.googleEventId").value("g3"));
        send(post("/api/calendar/v1/events"), """
                {"sourceId":"%s","title":"Party","allDay":true,"start":"2026-10-05","end":"2026-10-05"}"""
                .formatted(sourceId("holidays@group")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("calendar_read_only"));
        verify(google, times(1)).create(anyString(), anyString(), any());
    }

    @Test
    void aDeadGoogleCredentialAsksForReconnectionAndNeverSignsTheUserOut() throws Exception {
        connectGoogle();
        var sourceId = sourceId(EMAIL);
        when(oauth.refresh("refresh-token")).thenThrow(
                new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, "Reconnect this account."));
        // A fresh token is needed: the cached one is forgotten when Google refuses the account.
        when(google.calendars("google-token")).thenThrow(
                new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT, "Google access expired."));

        perform(post("/api/calendar/v1/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].status").value("needs_reconnect"));
        send(post("/api/calendar/v1/events"), """
                {"sourceId":"%s","title":"Lunch","allDay":true,"start":"2026-10-05","end":"2026-10-05"}"""
                .formatted(sourceId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("google_reconnect_required"));
        perform(get("/api/calendar/v1/calendar" + RANGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].authStatus").value("needs_reconnect"))
                .andExpect(jsonPath("$.events", hasSize(3)));
    }

    @Test
    void rejectsInvalidRequestsWithBoundedErrors() throws Exception {
        perform(get("/api/calendar/v1/events?from=2026-10-10&to=2026-01-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_range"));
        send(post("/api/calendar/v1/events"), """
                {"sourceId":"nope","title":"x","allDay":false,"start":"tomorrow","end":"later"}""")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_event_time"));
        send(post("/api/calendar/v1/events"), """
                {"sourceId":"nope","title":"x","allDay":true,"start":"2026-10-10","end":"2026-10-10"}""")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("source_not_found"));
        send(patch("/api/calendar/v1/accounts/missing"), "{\"name\":\"x\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("does not exist")));
    }
}
