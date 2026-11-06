package com.yourlifeone.planner.tasks.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.planner.testing.PlannerRequests;
import com.yourlifeone.planner.testing.PlannerTest;
import com.yourlifeone.planner.testing.TestClock.MovableClock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tasks, completion rewards and the daily reset through the full request path. */
@PlannerTest
class TasksApiIntegrationTest {
    private static final String BASE = "/api/planner/v1";
    private static final String LONDON = "{\"timeZone\":\"Europe/London\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovableClock clock;

    private PlannerRequests requests;

    @BeforeEach
    void setUp() {
        requests = new PlannerRequests(mockMvc);
        clock.set(Instant.parse("2026-09-27T12:00:00Z"));
    }

    private void save(UUID user, String id, String body) throws Exception {
        requests.as(user, put(BASE + "/tasks/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private static String habit(String title) {
        return "{\"title\":\"" + title + "\",\"priority\":\"high\",\"category\":\"daily\",\"recurring\":{\"frequency\":\"daily\"}}";
    }

    @Test
    void aSavedTaskIsNormalisedAndKeepsItsCompletion() throws Exception {
        var user = UUID.randomUUID();
        save(user, "t1", "{\"title\":\" Pay rent \",\"priority\":\"medium\",\"category\":\"task\",\"dueDate\":\"2026-10-01\"}");
        requests.as(user, post(BASE + "/tasks/t1/complete").contentType(MediaType.APPLICATION_JSON).content(LONDON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.completedLocalDate").value("2026-09-27"))
                .andExpect(jsonPath("$.reward.xpEarned").value(10))
                .andExpect(jsonPath("$.profile.totalXp").value(10));
        save(user, "t1", "{\"title\":\"Pay the rent\",\"priority\":\"high\",\"category\":\"task\"}");

        requests.as(user, get(BASE + "/tasks"))
                .andExpect(jsonPath("$.tasks[0].title").value("Pay the rent"))
                .andExpect(jsonPath("$.tasks[0].completed").value(true));
        requests.as(user, put(BASE + "/tasks/p1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"high\",\"category\":\"prayer\",\"prayerName\":\"Fajr\"}"))
                .andExpect(jsonPath("$.title").value("Fajr Prayer"))
                .andExpect(jsonPath("$.recurring.frequency").value("daily"));
        requests.as(user, put(BASE + "/tasks/bad").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"priority\":\"urgent\",\"category\":\"task\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_priority"));
    }

    @Test
    void aHabitEarnsXpOnceADayAndStaysDoneUntilTheReset() throws Exception {
        var user = UUID.randomUUID();
        save(user, "water", habit("Water"));
        requests.as(user, post(BASE + "/tasks/water/complete").contentType(MediaType.APPLICATION_JSON).content(LONDON))
                .andExpect(jsonPath("$.reward.xpEarned").value(25));
        requests.as(user, post(BASE + "/tasks/water/reopen")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("habit_locked"));

        // The next day in London the reset reopens it, and completing it earns XP again.
        clock.set(Instant.parse("2026-09-27T23:30:00Z"));
        requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appTimeZone\":\"Europe/London\",\"prayerTimeZone\":\"Europe/London\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].completed").value(false))
                .andExpect(jsonPath("$.tasks[0].recurring.lastReset").value("2026-09-28"));
        requests.as(user, post(BASE + "/tasks/water/complete").contentType(MediaType.APPLICATION_JSON).content(LONDON))
                .andExpect(jsonPath("$.reward.xpEarned").value(25))
                .andExpect(jsonPath("$.profile.currentStreak").value(2));
    }

    @Test
    void theResetUsesTheUsersTimeZoneNotTheServers() throws Exception {
        var user = UUID.randomUUID();
        save(user, "read", habit("Read"));
        clock.set(Instant.parse("2026-09-27T20:00:00Z"));
        requests.as(user, post(BASE + "/tasks/read/complete").contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeZone\":\"America/Los_Angeles\"}")).andExpect(status().isOk());

        // Midnight UTC is still the 27th in Los Angeles: nothing reopens, however often the reset runs.
        clock.set(Instant.parse("2026-09-28T01:00:00Z"));
        for (int run = 0; run < 2; run++) {
            requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appTimeZone\":\"America/Los_Angeles\",\"prayerTimeZone\":null}"))
                    .andExpect(jsonPath("$.tasks[0].completed").value(true));
        }
        clock.set(Instant.parse("2026-09-28T08:00:00Z"));
        requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appTimeZone\":\"America/Los_Angeles\",\"prayerTimeZone\":null}"))
                .andExpect(jsonPath("$.tasks[0].completed").value(false));
    }

    @Test
    void aMissedDayEndsTheStreak() throws Exception {
        var user = UUID.randomUUID();
        save(user, "t", "{\"title\":\"One\",\"priority\":\"low\",\"category\":\"task\"}");
        requests.as(user, post(BASE + "/tasks/t/complete").contentType(MediaType.APPLICATION_JSON).content(LONDON));

        clock.set(Instant.parse("2026-09-29T12:00:00Z"));
        requests.as(user, post(BASE + "/day/rollover").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appTimeZone\":\"Europe/London\"}"))
                .andExpect(jsonPath("$.profile.currentStreak").value(0))
                .andExpect(jsonPath("$.profile.longestStreak").value(1));
    }

    @Test
    void prayerHabitsCompleteThroughThePrayerServiceOnly() throws Exception {
        var user = UUID.randomUUID();
        save(user, "fajr", "{\"priority\":\"high\",\"category\":\"prayer\",\"prayerName\":\"Fajr\"}");

        requests.as(user, post(BASE + "/tasks/fajr/complete").contentType(MediaType.APPLICATION_JSON).content(LONDON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("prayer_task"));
        requests.as(user, put(BASE + "/tasks/fajr-2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"high\",\"category\":\"prayer\",\"prayerName\":\"Fajr\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("prayer_habit_exists"));
    }

    @Test
    void concurrentCompletionsOfOneTaskRewardItOnce() throws Exception {
        var user = UUID.randomUUID();
        save(user, "goal", "{\"title\":\"Hajj\",\"priority\":\"medium\",\"category\":\"goal\"}");

        var results = PlannerRequests.concurrently(6, attempt -> requests.as(user, post(BASE + "/tasks/goal/complete")
                .header(IdempotencyKeys.HEADER, "complete-" + attempt).contentType(MediaType.APPLICATION_JSON)
                .content(LONDON)));

        assertEquals(1, results.stream().filter(result -> result.getResponse().getStatus() == 200).count());
        requests.as(user, get(BASE + "/gamification")).andExpect(jsonPath("$.totalXp").value(60));
    }

    @Test
    void concurrentCreatesOfOneTaskMakeOneTaskAndARetryIsReplayed() throws Exception {
        var user = UUID.randomUUID();
        var results = PlannerRequests.concurrently(8, attempt -> requests.as(user, put(BASE + "/tasks/same")
                .header(IdempotencyKeys.HEADER, "task-" + attempt).contentType(MediaType.APPLICATION_JSON)
                .content(habit("Walk"))));

        for (var result : results) assertEquals(200, result.getResponse().getStatus());
        requests.as(user, get(BASE + "/tasks")).andExpect(jsonPath("$.tasks.length()").value(1));
        requests.as(user, put(BASE + "/tasks/same").header(IdempotencyKeys.HEADER, "task-0")
                        .contentType(MediaType.APPLICATION_JSON).content(habit("Walk")))
                .andExpect(header().string(IdempotencyKeys.REPLAYED_HEADER, "true"));
        requests.as(user, put(BASE + "/tasks/same").header(IdempotencyKeys.HEADER, "task-0")
                        .contentType(MediaType.APPLICATION_JSON).content(habit("Run")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void removingAProjectTakesItsTasksOffTheBoard() throws Exception {
        var user = UUID.randomUUID();
        save(user, "b1", "{\"title\":\"Ship\",\"priority\":\"high\",\"category\":\"task\",\"projectId\":\"p1\"}");

        requests.as(user, get(BASE + "/tasks")).andExpect(jsonPath("$.tasks[0].workflowState").value("backlog"));
        requests.as(user, post(BASE + "/tasks/unlink-project").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"p1\"}"))
                .andExpect(jsonPath("$.tasks[0].projectId").doesNotExist())
                .andExpect(jsonPath("$.tasks[0].workflowState").doesNotExist());
        requests.as(user, delete(BASE + "/tasks/b1")).andExpect(status().isNoContent());
        requests.as(user, delete(BASE + "/tasks/b1")).andExpect(status().isNotFound());
    }
}
