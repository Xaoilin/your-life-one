package com.yourlifeone.planner.gamification.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.planner.testing.PlannerRequests;
import com.yourlifeone.planner.testing.PlannerTest;
import com.yourlifeone.planner.testing.PrayerOutcomes;
import com.yourlifeone.planner.testing.TestClock.MovableClock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A prayer the prayer service recorded earns XP exactly once, and only then. */
@PlannerTest
class PrayerRewardsIntegrationTest {
    private static final String BASE = "/api/planner/v1";
    private static final LocalDate TODAY = LocalDate.parse("2026-09-27");
    private static final Instant PRAYED_AT = Instant.parse("2026-09-27T05:10:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovableClock clock;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PrayerOutcomes outcomes;

    private PlannerRequests requests;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        requests = new PlannerRequests(mockMvc);
        clock.set(Instant.parse("2026-09-27T06:00:00Z"));
        requests.as(user, put(BASE + "/tasks/fajr").contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\":\"high\",\"category\":\"prayer\",\"prayerName\":\"Fajr\"}")).andExpect(status().isOk());
        requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                .content("{\"appTimeZone\":\"Europe/London\",\"prayerTimeZone\":\"Europe/London\"}")).andExpect(status().isOk());
    }

    private String sync(String key) throws Exception {
        return requests.as(user, post(BASE + "/rewards/prayers/sync").header(IdempotencyKeys.HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void aPrayerIsRewardedOnlyOnceTheServiceRecordedIt() throws Exception {
        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"))
                .andExpect(jsonPath("$.reward").doesNotExist())
                .andExpect(jsonPath("$.profile.totalXp").value(0));

        outcomes.prayed(user, TODAY, "Fajr", PRAYED_AT, "fajr");
        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"))
                .andExpect(jsonPath("$.reward.xpEarned").value(25))
                .andExpect(jsonPath("$.task.completed").value(true))
                .andExpect(jsonPath("$.task.recurring.lastReset").value("2026-09-27"))
                .andExpect(jsonPath("$.profile.totalXp").value(25))
                .andExpect(jsonPath("$.profile.habitTallies.fajr").value(1));

        // Asking again reports the same reward and grants nothing more.
        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"))
                .andExpect(jsonPath("$.reward.xpEarned").value(25))
                .andExpect(jsonPath("$.task").doesNotExist())
                .andExpect(jsonPath("$.profile.totalXp").value(25));
    }

    @Test
    void concurrentSyncsGrantTheRewardOnce() throws Exception {
        outcomes.prayed(user, TODAY, "Fajr", PRAYED_AT, "fajr");

        var results = PlannerRequests.concurrently(6, attempt -> requests.as(user, post(BASE + "/rewards/prayers/sync")
                .header(IdempotencyKeys.HEADER, "sync-" + attempt).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}")));

        for (var result : results) assertEquals(200, result.getResponse().getStatus());
        requests.as(user, get(BASE + "/gamification")).andExpect(jsonPath("$.totalXp").value(25));
        assertEquals(1, JdbcClient.create(dataSource).sql("SELECT count(*) FROM gamification.prayer_reward WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }

    @Test
    void anUndonePrayerGivesItsRewardBackAndReopensTheHabit() throws Exception {
        outcomes.prayed(user, TODAY, "Fajr", PRAYED_AT, "fajr");
        sync("first");

        outcomes.undone(user, TODAY, "Fajr");
        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"))
                .andExpect(jsonPath("$.reward").doesNotExist())
                .andExpect(jsonPath("$.task.completed").value(false))
                .andExpect(jsonPath("$.profile.totalXp").value(0))
                .andExpect(jsonPath("$.profile.totalTasksCompleted").value(0));
    }

    @Test
    void importedRewardsAreHistoryAndNeverGrantedOrTakenBackAgain() throws Exception {
        var jdbc = JdbcClient.create(dataSource);
        jdbc.sql("""
                INSERT INTO gamification.prayer_reward (user_id, prayer_date, prayer_name, source, log_id, xp_earned,
                    level, current_streak, recorded_at, granted_at)
                VALUES (?, '2026-09-20', 'Asr', 'import', 'prayer:asr', 25, 1, 1, now(), now())""").param(user).update();
        outcomes.prayed(user, LocalDate.parse("2026-09-20"), "Asr", Instant.parse("2026-09-20T15:00:00Z"), null);

        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.profile.totalXp").value(0));
        outcomes.undone(user, LocalDate.parse("2026-09-20"), "Asr");
        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.profile.totalXp").value(0));
        assertEquals(1, jdbc.sql("SELECT count(*) FROM gamification.prayer_reward WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }

    @Test
    void aResetKeepsPrayerReceiptsSoNothingIsRewardedAgain() throws Exception {
        outcomes.prayed(user, TODAY, "Fajr", PRAYED_AT, "fajr");
        sync("before-reset");

        requests.as(user, post(BASE + "/gamification/reset")).andExpect(status().isBadRequest());
        requests.as(user, post(BASE + "/gamification/reset").param("confirm", "true"))
                .andExpect(jsonPath("$.totalXp").value(0));
        requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appTimeZone\":\"Europe/London\"}"))
                .andExpect(jsonPath("$.profile.totalXp").value(0));
    }

    @Test
    void whenThePrayerServiceIsDownSyncIsUnavailableAndNoRewardIsTakenBack() throws Exception {
        outcomes.prayed(user, TODAY, "Fajr", PRAYED_AT, "fajr");
        sync("granted");

        outcomes.unavailableFor(user);
        requests.as(user, post(BASE + "/rewards/prayers/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("prayer_unavailable"));
        requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appTimeZone\":\"Europe/London\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.totalXp").value(25));
    }

    @Test
    void aRetriedSyncIsReplayedWithoutAskingThePrayerServiceAgain() throws Exception {
        outcomes.prayed(user, TODAY, "Fajr", PRAYED_AT, "fajr");
        var first = sync("same-key");

        outcomes.unavailableFor(user);
        assertEquals(first, sync("same-key"));
    }
}
