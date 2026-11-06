package com.yourlifeone.planner.clock.api.controller;

import com.yourlifeone.planner.testing.PlannerRequests;
import com.yourlifeone.planner.testing.PlannerTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The clock workspace and daily momentum through the full request path. */
@PlannerTest
class ClockAndMomentumIntegrationTest {
    private static final String CLOCK = "/api/planner/v1/clock";
    private static final String MOMENTUM = "/api/planner/v1/momentum";

    @Autowired
    private MockMvc mockMvc;

    private PlannerRequests requests;

    @BeforeEach
    void setUp() {
        requests = new PlannerRequests(mockMvc);
    }

    private static String timer(String status, Long endsAt) {
        return """
                {"durationMs":300000,"remainingMs":300000,"endsAt":%s,"status":"%s","sound":"chime","alerting":false}"""
                .formatted(endsAt, status);
    }

    @Test
    void newStopwatchesAndTimersAreNumberedAndKeptInOrder() throws Exception {
        var user = UUID.randomUUID();
        requests.as(user, put(CLOCK + "/stopwatches/s1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accumulatedMs\":0,\"startedAt\":null,\"laps\":[]}"))
                .andExpect(jsonPath("$.stopwatches[0].label").value("Stopwatch 1"))
                .andExpect(jsonPath("$.nextStopwatchNumber").value(2));
        requests.as(user, put(CLOCK + "/stopwatches/s2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Tea\",\"accumulatedMs\":1200,\"startedAt\":1790000000000,\"laps\":[400,800]}"));
        requests.as(user, put(CLOCK + "/timers/t1").contentType(MediaType.APPLICATION_JSON)
                .content(timer("running", 1790000300000L)))
                .andExpect(jsonPath("$.timers[0].label").value("Timer 1"));

        requests.as(user, get(CLOCK))
                .andExpect(jsonPath("$.stopwatches[1].laps[1]").value(800))
                .andExpect(jsonPath("$.stopwatches[1].label").value("Tea"))
                .andExpect(jsonPath("$.timers[0].endsAt").value(1790000300000L));
        requests.as(user, put(CLOCK + "/timers/t1").contentType(MediaType.APPLICATION_JSON).content(timer("idle", 1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_end"));
        requests.as(user, delete(CLOCK + "/stopwatches/s1")).andExpect(jsonPath("$.stopwatches.length()").value(1));
    }

    @Test
    void concurrentCreatesOfOneTimerMakeOneTimer() throws Exception {
        var user = UUID.randomUUID();
        var results = PlannerRequests.concurrently(6, attempt -> requests.as(user, put(CLOCK + "/timers/same")
                .header("Idempotency-Key", "timer-" + attempt).contentType(MediaType.APPLICATION_JSON)
                .content(timer("idle", null))));

        for (var result : results) assertEquals(200, result.getResponse().getStatus());
        requests.as(user, get(CLOCK)).andExpect(jsonPath("$.timers.length()").value(1))
                .andExpect(jsonPath("$.nextTimerNumber").value(2));
    }

    @Test
    void aMomentumSaveFromAnOutOfDateCopyIsRefused() throws Exception {
        var user = UUID.randomUUID();
        var state = "{\"schemaVersion\":2,\"templates\":[],\"logs\":{},\"reminderPreferences\":{}}";
        requests.as(user, put(MOMENTUM + "/learn").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":" + state + ",\"expectedVersion\":null}"))
                .andExpect(jsonPath("$.version").value(0));
        var progressed = state.replace("\"logs\":{}", "\"logs\":{\"2026-09-27:learn\":{\"progress\":{\"pages\":5}}}");
        requests.as(user, put(MOMENTUM + "/learn").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":" + progressed + ",\"expectedVersion\":0}"))
                .andExpect(jsonPath("$.version").value(1));

        requests.as(user, put(MOMENTUM + "/learn").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":" + state + ",\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("momentum_changed"));
        requests.as(user, put(MOMENTUM + "/sleep").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":" + state + ",\"expectedVersion\":null}"))
                .andExpect(status().isBadRequest());
        requests.as(user, get(MOMENTUM)).andExpect(jsonPath("$.pillars[0].pillar").value("learn"))
                .andExpect(jsonPath("$.pillars[0].state.logs['2026-09-27:learn'].progress.pages").value(5));
    }
}
