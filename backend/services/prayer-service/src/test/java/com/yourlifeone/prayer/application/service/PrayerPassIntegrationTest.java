package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.Fixtures;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.SentReminder;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.OutcomeRepository;
import com.yourlifeone.prayer.infrastructure.persistence.repository.ScheduleDayRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"spring.datasource.hikari.data-source-properties.sslmode=disable",
        "app.prayer.reminder-check-interval=PT24H"})
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerPassIntegrationTest {
    @Autowired private JdbcClient jdbc;
    @Autowired private SchemaMigrator migrator;
    @Autowired private ReminderEvaluationService evaluation;
    @Autowired private PrayerProfileService profiles;
    @Autowired private UserScheduleService userSchedules;
    @Autowired private ScheduleDayRepository schedules;
    @Autowired private PrayerPersistenceMapper mapper;
    @Autowired private OutcomeRepository outcomes;
    @Autowired private OutcomeService outcomeService;
    @Autowired private ReminderSnoozeService snoozes;
    @Autowired private JsonMapper json;
    @MockitoBean private ScheduleSource scheduleSource;
    @MockitoBean private Clock clock;
    @MockitoBean private ChangePublisher changes;

    private final List<UUID> owners = new ArrayList<>();
    private final List<UUID> scheduleIds = new ArrayList<>();
    private PrayerPassEvidence evidence;

    @BeforeEach
    void setUp() {
        at("2026-09-25T04:00:00Z");
        migrator.ensureMigrated();
        evidence = new PrayerPassEvidence(jdbc);
    }

    @AfterEach
    void cleanEveryInventedOwner() {
        for (var owner : owners) {
            for (var table : List.of("outcome", "notice", "reminder", "momentum_reminder", "user_day", "profile")) {
                jdbc.sql("DELETE FROM prayer." + table + " WHERE user_id = ?").param(owner).update();
                assertEquals(0L, jdbc.sql("SELECT count(*) FROM prayer." + table + " WHERE user_id = ?")
                        .param(owner).query(Long.class).single());
            }
            jdbc.sql("DELETE FROM auth.users WHERE id = ?").param(owner).update();
            assertEquals(0L, jdbc.sql("SELECT count(*) FROM auth.users WHERE id = ?")
                    .param(owner).query(Long.class).single());
        }
        scheduleIds.forEach(schedules::deleteById);
    }

    @Test
    void sharedReadsAreOncePerPassAndSnoozeClaimsStayFresh() {
        var user = owner("Europe/London");
        outcomes.saveAndFlush(mapper.toEntity(Fixtures.outcome(Fixtures.DAY, PrayerName.FAJR,
                OutcomeStatus.ON_TIME, clock.instant()), user));
        var first = measured(user, "active", "2026-09-25T11:53:00Z");
        var unchanged = measured(user, "unchanged", "2026-09-25T11:54:00Z");
        at("2026-09-25T11:54:00Z");
        snoozes.snooze(user, "opportunity:2026-09-25:Dhuhr");
        assertTrue(evaluation.claimDue(user, Instant.parse("2026-09-25T11:58:00Z")).isEmpty());
        var resend = measured(user, "resend", "2026-09-25T11:59:00Z");
        assertTrue(evaluation.claimDue(user, Instant.parse("2026-09-25T12:00:00Z")).isEmpty());
        System.out.println("PRAYER_PASS_PLAN_EVIDENCE " + json.writeValueAsString(Map.of(
                "indexes", evidence.indexes(), "plans", evidence.plans(user, first))));
        for (var pass : List.of(first, unchanged, resend)) {
            assertEquals(1, PrayerPassEvidence.calls(pass, "from\\s+prayer[.]user_day.*order by"));
            assertEquals(1, PrayerPassEvidence.calls(pass, "from\\s+prayer[.]user_day.*between"));
            assertEquals(1, PrayerPassEvidence.calls(pass, "from\\s+prayer[.]outcome"));
            assertEquals(1, PrayerPassEvidence.calls(pass, "from\\s+prayer[.]momentum_reminder"));
        }
    }

    @Test
    void aCompletionBetweenPassesSuppressesTheSnoozedPrayer() {
        var user = owner("Europe/London");
        assertEquals(2, evaluation.claimDue(user, Instant.parse("2026-09-25T11:53:00Z")).size());
        at("2026-09-25T11:54:00Z");
        snoozes.snooze(user, "opportunity:2026-09-25:Dhuhr");
        at("2026-09-25T11:55:00Z");
        outcomeService.record(user, Fixtures.DAY, PrayerName.DHUHR, OutcomeStatus.ON_TIME, "test", null);
        assertTrue(evaluation.claimDue(user, Instant.parse("2026-09-25T11:59:00Z")).isEmpty());
    }

    @Test
    void pairedCompletionLeavesTheOtherPrayerIndependent() {
        var user = owner("Europe/London");
        at("2026-09-25T11:53:00Z");
        outcomeService.record(user, Fixtures.DAY, PrayerName.ASR, OutcomeStatus.ON_TIME, "test", null);
        assertTrue(evaluation.claimDue(user, clock.instant()).stream()
                .anyMatch(reminder -> reminder.key().equals("opportunity:2026-09-25:Dhuhr")));
        assertTrue(evaluation.claimDue(user, Instant.parse("2026-09-25T15:09:00Z")).stream()
                .noneMatch(reminder -> reminder.key().equals("opportunity:2026-09-25:Asr")));
        assertTrue(evaluation.claimDue(user, Instant.parse("2026-09-25T18:00:00Z")).isEmpty());
    }

    @Test
    void olderUnexpiredReceiptsRefetchOutcomesOutsideThePassRange() {
        var user = owner("Europe/London");
        outcomes.saveAndFlush(mapper.toEntity(Fixtures.outcome(Fixtures.DAY.minusDays(2), PrayerName.FAJR,
                OutcomeStatus.ON_TIME, clock.instant()), user));
        jdbc.sql("""
                INSERT INTO prayer.notice (user_id, key, kind, prayer_date, prayer_name, fires_at, expires_at,
                    sent_at, snoozed_until, snooze_count)
                VALUES (?, 'opportunity:2026-09-23:Fajr', 'prayer-opportunity', '2026-09-23', 'Fajr',
                    '2026-09-23T04:12:00Z', '2026-09-25T12:22:00Z', '2026-09-23T04:13:00Z',
                    '2026-09-25T11:59:00Z', 1)
                """).param(user).update();
        var before = evidence.reads();
        assertTrue(evaluation.claimDue(user, Instant.parse("2026-09-25T11:59:00Z")).stream()
                .noneMatch(reminder -> reminder.key().equals("opportunity:2026-09-23:Fajr")));
        assertEquals(2, PrayerPassEvidence.calls(evidence.delta(before), "from\\s+prayer[.]outcome"));
    }

    @ParameterizedTest
    @CsvSource({"Europe/London,2026-09-25T05:40:00Z,deadline:2026-09-25:Fajr",
            "Europe/London,2026-09-25T04:13:00Z,opportunity:2026-09-25:Fajr",
            "Europe/London,2026-09-25T23:40:00Z,deadline:2026-09-25:Isha",
            "Asia/Dubai,2026-09-25T08:53:00Z,opportunity:2026-09-25:Dhuhr"})
    void preservesDueQuietMidnightAndZoneBoundaries(String zone, String instant, String key) {
        var user = owner(zone);
        var due = evaluation.claimDue(user, Instant.parse(instant));
        assertTrue(due.stream().map(SentReminder::key).toList().contains(key));
        assertTrue(due.stream().allMatch(reminder -> reminder.zone().equals(ZoneId.of(zone))));
        if (instant.contains("04:13")) assertEquals(1, due.size());
        assertTrue(evaluation.claimDue(user, Instant.parse(instant)).isEmpty());
    }

    private UUID owner(String zone) {
        var user = UUID.randomUUID();
        owners.add(user);
        jdbc.sql("INSERT INTO auth.users (id) VALUES (?)").param(user).update();
        profiles.getOrCreate(user);
        var day = Fixtures.london(Fixtures.DAY).toBuilder().city("KAN-331-" + user).zone(ZoneId.of(zone)).build();
        scheduleIds.add(day.id());
        schedules.saveAndFlush(mapper.toEntity(day, day.id()));
        userSchedules.remember(user, day);
        return user;
    }

    private List<Map<String, Object>> measured(UUID user, String label, String instant) {
        var before = evidence.reads();
        var due = evaluation.claimDue(user, Instant.parse(instant));
        var delta = evidence.delta(before);
        System.out.println("PRAYER_PASS_EVIDENCE " + json.writeValueAsString(Map.of("label", label,
                "reads", delta, "reminders", due)));
        return delta;
    }

    private void at(String instant) {
        when(clock.instant()).thenReturn(Instant.parse(instant));
    }
}
