package com.yourlifeone.planner.tasks.api.controller;

import com.yourlifeone.planner.testing.KnowledgeCounts;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Badges that count knowledge come from the knowledge service's counts; without them a completion still counts. */
@PlannerTest
class KnowledgeBadgesIntegrationTest {
    private static final String TASKS = "/api/planner/v1/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovableClock clock;

    @Autowired
    private KnowledgeCounts knowledge;

    private PlannerRequests requests;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        requests = new PlannerRequests(mockMvc);
        clock.set(Instant.parse("2026-09-27T12:00:00Z"));
        requests.as(user, put(TASKS + "/read").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Read a chapter\",\"priority\":\"low\",\"category\":\"task\"}")).andExpect(status().isOk());
    }


    @Test
    void aKnowledgeEntryEarnsItsBadgeOnTheNextCompletion() throws Exception {
        knowledge.entries(user, 1);

        requests.as(user, post(TASKS + "/read/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeZone\":\"Europe/London\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reward.newBadges", hasItem("first-note")));
    }

    @Test
    void whenTheKnowledgeServiceIsDownTheTaskCompletesAndItsBadgesWait() throws Exception {
        knowledge.entries(user, 1);
        knowledge.unavailableFor(user);

        requests.as(user, post(TASKS + "/read/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeZone\":\"Europe/London\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.completed").value(true))
                .andExpect(jsonPath("$.reward.xpEarned").isNumber())
                .andExpect(jsonPath("$.reward.newBadges", not(hasItem("first-note"))));
    }
}
