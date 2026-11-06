package com.yourlifeone.planner.api.controller;

import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.planner.testing.PlannerTest;
import com.yourlifeone.planner.testing.PrayerOutcomes;
import com.yourlifeone.planner.testing.TestClock.MovableClock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Provider side of the contract with the React app: responses keep the shape of their fixtures in
 * {@code contracts/planner-service}, which the app also uses as mocks.
 */
@PlannerTest
class PlannerContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/planner-service");
    private static final String BASE = "/api/planner/v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovableClock clock;

    @Autowired
    private PrayerOutcomes prayerOutcomes;

    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        clock.set(Instant.parse("2026-09-27T06:00:00Z"));
    }

    private void call(String name, String method, String path, MockHttpServletRequestBuilder request) throws Exception {
        var response = mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString()).tokenValue(user.toString()))))
                .andReturn().getResponse();
        CONTRACT.check(name, method, path, response.getStatus(), response.getContentAsString());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void taskResponsesKeepTheirShape() throws Exception {
        var tasks = BASE + "/tasks";
        call("task-saved", "PUT", tasks + "/rent", json(put(tasks + "/rent"), """
                {"title":"Pay rent","description":"","priority":"medium","category":"task"}"""));
        call("task-invalid", "PUT", tasks + "/bad", json(put(tasks + "/bad"), """
                {"title":"Bad","priority":"urgent","category":"task"}"""));
        mockMvc.perform(json(put(tasks + "/water"), """
                {"title":"Drink 1L water","priority":"high","category":"daily","recurring":{"frequency":"daily"},"emoji":"💧"}""")
                .with(jwt().jwt(token -> token.subject(user.toString()).tokenValue(user.toString()))));
        mockMvc.perform(json(put(tasks + "/fajr"), """
                {"priority":"high","category":"prayer","prayerName":"Fajr"}""")
                .with(jwt().jwt(token -> token.subject(user.toString()).tokenValue(user.toString()))));
        call("task-completed", "POST", tasks + "/water/complete",
                json(post(tasks + "/water/complete"), "{\"timeZone\":\"Europe/London\"}"));
        call("habit-locked", "POST", tasks + "/water/reopen", post(tasks + "/water/reopen"));
        call("task-already-completed", "POST", tasks + "/water/complete",
                json(post(tasks + "/water/complete"), "{\"timeZone\":\"Europe/London\"}"));
        call("task-reopened", "POST", tasks + "/rent/reopen", post(tasks + "/rent/reopen"));
        call("tasks", "GET", tasks, get(tasks));
        call("day-rolled-over", "POST", BASE + "/day/rollover", json(post(BASE + "/day/rollover"),
                "{\"appTimeZone\":\"Europe/London\",\"prayerTimeZone\":\"Europe/London\"}"));
        call("tasks-unlinked", "POST", tasks + "/unlink-project",
                json(post(tasks + "/unlink-project"), "{\"projectId\":\"agentboard\"}"));
        call("task-deleted", "DELETE", tasks + "/rent", delete(tasks + "/rent"));
    }

    @Test
    void progressResponsesKeepTheirShape() throws Exception {
        mockMvc.perform(json(put(BASE + "/tasks/fajr"), "{\"priority\":\"high\",\"category\":\"prayer\",\"prayerName\":\"Fajr\"}")
                .with(jwt().jwt(token -> token.subject(user.toString()).tokenValue(user.toString()))));
        prayerOutcomes.prayed(user, LocalDate.parse("2026-09-27"), "Fajr",
                Instant.parse("2026-09-27T05:10:00Z"), "fajr");
        call("prayer-rewards-synced", "POST", BASE + "/rewards/prayers/sync", json(post(BASE + "/rewards/prayers/sync"),
                "{\"date\":\"2026-09-27\",\"prayer\":\"Fajr\"}"));
        call("gamification", "GET", BASE + "/gamification", get(BASE + "/gamification"));
        call("progress-reset", "POST", BASE + "/gamification/reset?confirm=true",
                post(BASE + "/gamification/reset").param("confirm", "true"));

        var momentum = BASE + "/momentum";
        call("momentum-saved", "PUT", momentum + "/learn", json(put(momentum + "/learn"), """
                {"state":{"schemaVersion":2,"templates":[],"logs":{},"reminderPreferences":{}},"expectedVersion":null}"""));
        call("momentum-changed", "PUT", momentum + "/learn", json(put(momentum + "/learn"), """
                {"state":{"schemaVersion":2,"templates":[],"logs":{},"reminderPreferences":{}},"expectedVersion":7}"""));
        call("momentum", "GET", momentum, get(momentum));
    }

    @Test
    void clockResponsesKeepTheirShape() throws Exception {
        var clockPath = BASE + "/clock";
        call("stopwatch-saved", "PUT", clockPath + "/stopwatches/s1", json(put(clockPath + "/stopwatches/s1"), """
                {"label":"Tea","accumulatedMs":1200,"startedAt":1790000000000,"laps":[400,800]}"""));
        call("timer-saved", "PUT", clockPath + "/timers/t1", json(put(clockPath + "/timers/t1"), """
                {"label":"Pasta","durationMs":600000,"remainingMs":600000,"endsAt":1790000600000,"status":"running",
                 "sound":"bell","alerting":false,"completedAt":null}"""));
        call("clock", "GET", clockPath, get(clockPath));
        call("stopwatch-deleted", "DELETE", clockPath + "/stopwatches/s1", delete(clockPath + "/stopwatches/s1"));
        call("timer-deleted", "DELETE", clockPath + "/timers/t1", delete(clockPath + "/timers/t1"));
    }

    @Test
    void rateLimitedRequestsKeepTheirShape() throws Exception {
        var limited = UUID.randomUUID();
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(BASE + "/tasks");
        var response = mockMvc.perform(request.with(jwt().jwt(token -> token.subject(limited.toString()))))
                .andReturn().getResponse();
        for (int attempt = 0; attempt < 200 && response.getStatus() != 429; attempt++) {
            response = mockMvc.perform(get(BASE + "/tasks").with(jwt().jwt(token -> token.subject(limited.toString()))))
                    .andReturn().getResponse();
        }
        CONTRACT.check("rate-limited", "GET", BASE + "/tasks", response.getStatus(), response.getContentAsString());
    }
}
