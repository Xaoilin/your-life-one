package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.application.service.ReminderEvents;
import com.yourlifeone.prayer.application.service.ReminderScheduler;
import com.yourlifeone.prayer.domain.Fixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Prayer opportunity and momentum notices, snoozes and the reminders endpoints, decided by the scheduler (run
 * by hand here). Same configuration as {@code PrayerRemindersIntegrationTest}, so both share one context.
 * London on 25 Sep 2026 (BST): Fajr starts 04:12Z, Dhuhr 11:52Z (on time until 15:08Z), Asr 15:08Z,
 * Maghrib 18:12Z, Isha 19:18Z; an opportunity notice lasts 30 minutes, a snooze 5.
 */
@SpringBootTest(properties = {"spring.datasource.hikari.data-source-properties.sslmode=disable",
        "app.prayer.reminder-check-interval=PT24H"})
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerReminderApiIntegrationTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/prayer-service");
    private static final ContractFixtures LIVE_CONTRACT = new ContractFixtures("contracts/live-service");
    private static final String REMINDERS = "/api/prayer/v1/reminders";
    private static final String DHUHR_OPPORTUNITY = "opportunity:2026-09-25:Dhuhr";
    private static final Instant MAGHRIB = Instant.parse("2026-09-25T18:12:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReminderScheduler reminders;

    @Autowired
    private JsonMapper json;

    @Autowired
    private JdbcClient jdbc;

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
        at("2026-09-25T04:00:00Z");
        perform(get("/api/prayer/v1/dashboard?city=London&country=United Kingdom"));
        clearInvocations(changes);
    }

    private void at(String instant) {
        when(clock.instant()).thenReturn(Instant.parse(instant));
    }

    private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString())))).andReturn().getResponse();
    }

    private MockHttpServletResponse snooze(String key, String idempotencyKey) throws Exception {
        return perform(post(REMINDERS + "/{key}/snooze", key).header(IdempotencyKeys.HEADER, idempotencyKey));
    }

    private MockHttpServletResponse saveMomentum(String body) throws Exception {
        return perform(put(REMINDERS + "/momentum").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Runs the scheduler at that instant and returns what it published of that type. */
    private List<JsonNode> sendAt(String instant, String type) {
        clearInvocations(changes);
        at(instant);
        reminders.sendDueReminders();
        return Mockito.mockingDetails(changes).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("publish") && call.getArguments().length == 3)
                .filter(call -> user.equals(call.getArgument(0)) && type.equals(call.getArgument(1)))
                .map(call -> (JsonNode) call.getArgument(2))
                .toList();
    }

    private static List<String> keys(List<JsonNode> events) {
        return events.stream().map(event -> event.get("key").asString()).sorted().toList();
    }

    private static List<JsonNode> elements(JsonNode array) {
        var elements = new ArrayList<JsonNode>();
        array.forEach(elements::add);
        return elements;
    }

    private static String momentumKey(Instant firesAt, String pillars) {
        return "momentum:2026-09-25:" + firesAt.toEpochMilli() + ":" + pillars;
    }

    @Test
    void opportunityAndMomentumNoticesAreSentOnceWithTheShapeTheAppReads() {
        var dhuhr = Instant.parse("2026-09-25T11:52:00Z");
        var sent = sendAt("2026-09-25T11:53:00Z", ReminderEvents.NOTICE);

        assertEquals(List.of(momentumKey(dhuhr, "learn"), DHUHR_OPPORTUNITY), keys(sent));
        var momentum = sent.stream().filter(event -> event.get("kind").asString().equals("momentum")).findFirst()
                .orElseThrow();
        var event = json.createObjectNode().put("type", ReminderEvents.NOTICE).put("domain", "prayer")
                .put("at", "2026-09-25T11:53:00Z");
        event.set("data", momentum);
        LIVE_CONTRACT.check("prayer-notice-event", "GET", "/api/live/v1/events", 200, event.toString());
        assertEquals("Dhuhr|2026-09-25T18:12:00Z|Europe/London", momentum.get("prayer").asString() + "|"
                + momentum.get("expiresAt").asString() + "|" + momentum.get("timeZone").asString());

        assertEquals(List.of(), sendAt("2026-09-25T11:54:00Z", ReminderEvents.NOTICE));
    }

    @Test
    void pillarsPromptedAtTheSameInstantShareOneNotice() {
        var sent = sendAt("2026-09-25T18:13:00Z", ReminderEvents.NOTICE);

        assertEquals(List.of(momentumKey(MAGHRIB, "learn+move"), "opportunity:2026-09-25:Maghrib"), keys(sent));
    }

    @Test
    void aRecordedPrayerAndACompletedPillarGetNoNotice() throws Exception {
        at("2026-09-25T11:53:00Z");
        assertEquals(201, perform(post("/api/prayer/v1/outcomes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-09-25\",\"prayer\":\"Dhuhr\",\"status\":\"on_time\"}")).getStatus());
        assertEquals(200, saveMomentum("""
                {"pillars":[{"pillar":"learn","enabled":true,"afterPrayers":["Dhuhr","Maghrib","Isha"],
                             "completedOn":"2026-09-25"}]}""").getStatus());

        assertEquals(List.of(), sendAt("2026-09-25T11:53:30Z", ReminderEvents.NOTICE));
        assertEquals(List.of("opportunity:2026-09-25:Maghrib", momentumKey(MAGHRIB, "move")).stream().sorted().toList(),
                keys(sendAt("2026-09-25T18:13:00Z", ReminderEvents.NOTICE)));
    }

    @Test
    void momentumStaysQuietDuringTheQuietHours() throws Exception {
        at("2026-09-25T04:00:00Z");
        saveMomentum("""
                {"pillars":[{"pillar":"learn","enabled":true,"afterPrayers":["Fajr"],"completedOn":null},
                            {"pillar":"move","enabled":false,"afterPrayers":[],"completedOn":null}]}""");

        assertEquals(List.of("opportunity:2026-09-25:Fajr"), keys(sendAt("2026-09-25T04:13:00Z", ReminderEvents.NOTICE)));
    }

    @Test
    void aSnoozedNoticeIsHiddenThenSentOnceMoreWhenTheSnoozeEnds() throws Exception {
        sendAt("2026-09-25T11:53:00Z", ReminderEvents.NOTICE);

        at("2026-09-25T11:54:00Z");
        var snoozed = snooze(DHUHR_OPPORTUNITY, "snooze-1");
        CONTRACT.check("reminder-snoozed", "POST", REMINDERS + "/{key}/snooze", snoozed.getStatus(),
                snoozed.getContentAsString());
        assertEquals("2026-09-25T11:59:00Z|1", json.readTree(snoozed.getContentAsString()).get("snoozedUntil").asString()
                + "|" + json.readTree(snoozed.getContentAsString()).get("snoozeCount").asInt());
        var again = snooze(DHUHR_OPPORTUNITY, "snooze-2");
        CONTRACT.check("snooze-used", "POST", REMINDERS + "/{key}/snooze", again.getStatus(), again.getContentAsString());

        at("2026-09-25T11:55:00Z");
        var active = json.readTree(perform(get(REMINDERS)).getContentAsString()).get("active");
        assertEquals(List.of(momentumKey(Instant.parse("2026-09-25T11:52:00Z"), "learn")),
                keys(elements(active)));

        assertEquals(List.of(DHUHR_OPPORTUNITY), keys(sendAt("2026-09-25T11:59:30Z", ReminderEvents.NOTICE)));
        assertEquals(List.of(), sendAt("2026-09-25T12:00:00Z", ReminderEvents.NOTICE));
    }

    @Test
    void aSnoozeThatWouldOutlastTheReminderIsRefused() throws Exception {
        sendAt("2026-09-25T11:53:00Z", ReminderEvents.NOTICE);

        at("2026-09-25T12:18:00Z");
        var refused = snooze(DHUHR_OPPORTUNITY, "too-late");

        CONTRACT.check("snooze-too-late", "POST", REMINDERS + "/{key}/snooze", refused.getStatus(),
                refused.getContentAsString());
        assertEquals(409, refused.getStatus());
    }

    @Test
    void unknownAndExpiredRemindersCannotBeSnoozed() throws Exception {
        sendAt("2026-09-25T11:53:00Z", ReminderEvents.NOTICE);

        at("2026-09-25T11:54:00Z");
        var unknown = snooze("opportunity:2026-09-25:Asr", "unknown");
        CONTRACT.check("reminder-not-found", "POST", REMINDERS + "/{key}/snooze", unknown.getStatus(),
                unknown.getContentAsString());
        assertEquals(404, snooze("not-a-key", "malformed").getStatus());

        at("2026-09-25T12:30:00Z");
        assertEquals(404, snooze(DHUHR_OPPORTUNITY, "expired").getStatus());
    }

    @Test
    void deadlineAndMomentumRemindersCanBeSnoozedToo() throws Exception {
        assertEquals(1, sendAt("2026-09-25T05:40:00Z", ReminderEvents.REMINDER).size());
        at("2026-09-25T05:41:00Z");
        assertEquals(200, snooze("deadline:2026-09-25:Fajr", "fajr").getStatus());
        var resent = sendAt("2026-09-25T05:46:30Z", ReminderEvents.REMINDER);
        assertEquals("deadline:2026-09-25:Fajr|1", resent.getFirst().get("key").asString() + "|"
                + resent.getFirst().get("snoozeCount").asInt());

        sendAt("2026-09-25T18:13:00Z", ReminderEvents.NOTICE);
        at("2026-09-25T18:14:00Z");
        assertEquals(200, snooze(momentumKey(MAGHRIB, "learn+move"), "together").getStatus());
        assertEquals(List.of(momentumKey(MAGHRIB, "learn+move")),
                keys(sendAt("2026-09-25T18:19:30Z", ReminderEvents.NOTICE)));
    }

    @Test
    void onlyOneOfTwoSnoozesAtOnceSucceeds() throws Exception {
        sendAt("2026-09-25T11:53:00Z", ReminderEvents.NOTICE);
        at("2026-09-25T11:54:00Z");

        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var attempts = new ArrayList<Future<Integer>>();
            for (var key : List.of("race-1", "race-2")) {
                Callable<Integer> attempt = () -> {
                    start.await();
                    return snooze(DHUHR_OPPORTUNITY, key).getStatus();
                };
                attempts.add(pool.submit(attempt));
            }
            start.countDown();
            var statuses = new ArrayList<Integer>();
            for (var attempt : attempts) statuses.add(attempt.get());
            assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void remindersAndMomentumPreferencesKeepTheirShape() throws Exception {
        sendAt("2026-09-25T11:53:00Z", ReminderEvents.NOTICE);
        at("2026-09-25T11:54:00Z");

        var saved = saveMomentum("""
                {"pillars":[{"pillar":"move","enabled":true,"afterPrayers":["Asr","Isha"],"completedOn":"2026-09-24"}]}""");
        CONTRACT.check("momentum-reminders-saved", "PUT", REMINDERS + "/momentum", saved.getStatus(),
                saved.getContentAsString());
        var reminders = perform(get(REMINDERS));
        CONTRACT.check("reminders", "GET", REMINDERS, reminders.getStatus(), reminders.getContentAsString());

        var body = json.readTree(reminders.getContentAsString());
        assertEquals(2, body.get("active").size());
        assertEquals("Asr,Isha", String.join(",", elements(body.get("momentum").get(1).get("afterPrayers")).stream()
                .map(JsonNode::asString).toList()));
        assertEquals(400, saveMomentum("{\"pillars\":[{\"pillar\":\"pray\",\"enabled\":true,\"afterPrayers\":[]}]}")
                .getStatus());
    }
}
