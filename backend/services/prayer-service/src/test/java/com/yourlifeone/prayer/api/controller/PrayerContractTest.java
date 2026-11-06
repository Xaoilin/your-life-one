package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.Fixtures;
import com.yourlifeone.prayer.competition.api.response.CompetitionSummary;
import com.yourlifeone.prayer.competition.infrastructure.CompetitionPeer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Provider side of the contract with the React app: every response the app relies on must keep
 * the shape of its fixture in {@code contracts/prayer-service}, which the app also uses as mocks.
 */
@SpringBootTest(properties = {"spring.datasource.hikari.data-source-properties.sslmode=disable",
        "app.prayer.competition.owner-id=11111111-1111-4111-8111-111111111111",
        "app.prayer.competition.read-key=test-000000000000000000000000000000000000000000000000",
        "app.prayer.competition.peer-url=https://partner.example.test/api/prayer/v1/competition/summary",
        "app.prayer.competition.peer-key=test-000000000000000000000000000000000000000000000000"})
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class PrayerContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/prayer-service");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String LONDON = "?city=London&country=United Kingdom";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleSource scheduleSource;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private CompetitionPeer competitionPeer;

    private final UUID user = UUID.randomUUID();

    private MvcResult call(String name, String method, String pathTemplate, MockHttpServletRequestBuilder request,
            UUID as) throws Exception {
        var result = mockMvc.perform(request.with(jwt().jwt(token -> token.subject(as.toString())))).andReturn();
        var response = result.getResponse();
        CONTRACT.check(name, method, pathTemplate, response.getStatus(), response.getContentAsString());
        return result;
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void everyResponseTheReactAppUsesKeepsItsContractShape() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-09-25T12:00:00Z"));
        when(scheduleSource.fetch(anyString(), anyString(), any())).thenAnswer(call -> Fixtures
                .london(call.getArgument(2, LocalDate.class)).toBuilder().id(null)
                .city(call.getArgument(0)).country(call.getArgument(1)).build());

        call("preferences", "GET", "/api/prayer/v1/preferences", get("/api/prayer/v1/preferences"), user);
        call("preferences-updated", "PUT", "/api/prayer/v1/preferences", json(put("/api/prayer/v1/preferences"),
                "{\"enabled\":true,\"reminderEnabled\":true,\"reminderMinutes\":15}"), user);
        call("schedule", "GET", "/api/prayer/v1/schedule", get("/api/prayer/v1/schedule" + LONDON), user);
        // The app loads the dashboard first, which records today's timetable for the user.
        mockMvc.perform(get("/api/prayer/v1/dashboard" + LONDON).with(jwt().jwt(token -> token.subject(user.toString()))));

        var created = call("outcome-created", "POST", "/api/prayer/v1/outcomes", json(post("/api/prayer/v1/outcomes"),
                "{\"date\":\"2026-09-25\",\"prayer\":\"Dhuhr\",\"status\":\"on_time\",\"source\":\"tasks\",\"taskId\":\"task-dhuhr\"}"), user);
        var outcomeId = JSON.readTree(created.getResponse().getContentAsString()).path("outcome").path("id").asString();
        call("outcome-exists", "POST", "/api/prayer/v1/outcomes", json(post("/api/prayer/v1/outcomes"),
                "{\"date\":\"2026-09-25\",\"prayer\":\"Dhuhr\",\"status\":\"late\"}"), user);
        call("outcome-corrected", "PATCH", "/api/prayer/v1/outcomes/{id}",
                json(patch("/api/prayer/v1/outcomes/" + outcomeId), "{\"status\":\"late\",\"source\":\"history\"}"), user);

        call("dashboard", "GET", "/api/prayer/v1/dashboard", get("/api/prayer/v1/dashboard" + LONDON), user);
        call("outcomes", "GET", "/api/prayer/v1/outcomes",
                get("/api/prayer/v1/outcomes?from=2026-09-01&to=2026-09-30"), user);
        call("rewarded-outcomes", "GET", "/api/prayer/v1/outcomes/rewarded", get("/api/prayer/v1/outcomes/rewarded"), user);
        call("stats", "GET", "/api/prayer/v1/stats", get("/api/prayer/v1/stats?from=2026-09-01&to=2026-09-30"), user);

    }

    @Test
    void prayerCompetitionUsesAnExplicitPairAndAReadOnlyCapability() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-04T12:00:00Z"));
        var owner = UUID.fromString("11111111-1111-4111-8111-111111111111");
        var from = LocalDate.parse("2026-11-01");
        var to = LocalDate.parse("2026-11-30");
        var partner = CompetitionSummary.builder().from(from).to(to).asOf(clock.instant()).onTime(8).late(1).missed(1)
                .pointsEarned(74).pointsAvailable(120).allPrayed(false).build();
        when(competitionPeer.read(from, to)).thenReturn(partner);
        var path = "/api/prayer/v1/competition";
        var query = "?from=2026-11-01&to=2026-11-30";
        call("competition", "GET", path, get(path + query), owner);
        call("competition-weighted", "GET", path, get(path + query), owner);
        call("competition-not-connected", "GET", path, get(path + query), user);
        var key = "test-000000000000000000000000000000000000000000000000";
        var shared = mockMvc.perform(get(path + "/summary" + query).header("X-Prayer-Competition-Key", key))
                .andExpect(status().isOk()).andReturn();
        CONTRACT.check("competition-summary", "GET", path + "/summary", 200, shared.getResponse().getContentAsString());
        CONTRACT.check("competition-summary-weighted", "GET", path + "/summary", 200, shared.getResponse().getContentAsString());
        assertThat(shared.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(shared.getResponse().getContentAsString()).doesNotContain("userId", "cycle", "notObligatory", "prayers");
        mockMvc.perform(get(path + "/summary" + query)).andExpect(status().is4xxClientError());
        mockMvc.perform(get(path + "/summary" + query).header("X-Prayer-Competition-Key", "wrong"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get(path + "/summary" + query).with(jwt().jwt(token -> token.subject(owner.toString()))))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post(path + "/summary" + query).header("X-Prayer-Competition-Key", key))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/prayer/v1/outcomes" + query).header("X-Prayer-Competition-Key", key))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(path + query)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path + query).with(jwt().jwt(token -> token.subject(owner.toString()).claim("client_id", "unapproved-agent"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(path + "?from=2026-01-01&to=2026-11-30").with(jwt().jwt(token -> token.subject(owner.toString()))))
                .andExpect(status().isBadRequest());
    }
    @Test
    void explicitTimingKeepsItsAdditiveConsumerContract() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2026-09-25T12:00:00Z"));
        call("outcome-created-timing", "POST", "/api/prayer/v1/outcomes",
                json(post("/api/prayer/v1/outcomes"), CONTRACT.requestBody("outcome-created-timing")), user);
    }

}
