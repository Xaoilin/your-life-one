package com.yourlifeone.profile.api.controller;

import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.profile.activity.api.controller.ActivityEventJson;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Provider side of the contract with the React app: responses keep the shape of their fixtures in
 * {@code contracts/profile-service}, which the app also uses as mocks.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class ProfileContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/profile-service");
    private static final String SETTINGS = "/api/profile/v1/settings";
    private static final String PREFERENCES = "/api/profile/v1/preferences";
    private static final String INTEGRATIONS = "/api/profile/v1/integrations";
    private static final String EVENTS = "/api/profile/v1/operational-events";
    private static final String ACTIVITY_EVENTS = "/api/profile/v1/activity/events";
    private static final String ACTIVITY_INSIGHTS = "/api/profile/v1/activity/insights";

    @Autowired
    private MockMvc mockMvc;

    private final UUID user = UUID.randomUUID();

    private void call(String name, String method, MockHttpServletRequestBuilder request) throws Exception {
        call(name, method, SETTINGS, request);
    }

    private void call(String name, String method, String path, MockHttpServletRequestBuilder request)
            throws Exception {
        var response = mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString()))))
                .andReturn().getResponse();
        CONTRACT.check(name, method, path, response.getStatus(), response.getContentAsString());
    }

    @Test
    void softwareEventsKeepTheSharedIngestContractAndAppearInInsights() throws Exception {
        var event = ActivityEventJson.event(UUID.randomUUID(), 1, "navigation", "navigation", "viewed")
                .with("surface", "software");

        call("activity-events-accepted", "POST", ACTIVITY_EVENTS, post(ACTIVITY_EVENTS)
                .contentType(MediaType.APPLICATION_JSON).content(ActivityEventJson.batch(List.of(event))));

        mockMvc.perform(get(ACTIVITY_INSIGHTS).param("surface", "software")
                .with(jwt().jwt(token -> token.subject(user.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.eventCount").value(1))
                .andExpect(jsonPath("$.summary.activeSurfaceCount").value(1));
    }

    @Test
    void browserMeasurementsKeepTheSharedRequestAndResponseContract() throws Exception {
        var body = CONTRACT.requestBody("operational-measurement-accepted")
                .replace("2026-10-21T00:00:00.000Z", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        call("operational-measurement-accepted", "POST", EVENTS,
                post(EVENTS).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void everyResponseTheReactAppUsesKeepsItsContractShape() throws Exception {
        call("settings-default", "GET", get(SETTINGS));
        call("settings-updated", "PUT", put(SETTINGS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"city\":\"London\",\"country\":\"United Kingdom\",\"timeZone\":\"Europe/London\"}"));
        call("settings-invalid", "PUT", put(SETTINGS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"city\":\"London\",\"country\":\"United Kingdom\",\"timeZone\":\"+01:00\"}"));
    }

    @Test
    void preferencesIntegrationsAndOperationalEventsKeepTheirContractShape() throws Exception {
        call("preferences-default", "GET", PREFERENCES, get(PREFERENCES));
        call("preferences-updated", "PUT", PREFERENCES, put(PREFERENCES).contentType(MediaType.APPLICATION_JSON)
                .content("{\"theme\":\"dark\",\"dataRetentionDays\":30,\"telemetry\":true,"
                        + "\"defaultCalendarTab\":\"agenda\",\"goalTags\":[\"Health\",\"Work\"]}"));
        call("preferences-invalid", "PUT", PREFERENCES, put(PREFERENCES).contentType(MediaType.APPLICATION_JSON)
                .content("{\"theme\":\"dark\",\"dataRetentionDays\":1,\"telemetry\":true,\"goalTags\":[]}"));
        call("integration-saved", "PUT", INTEGRATIONS + "/google", put(INTEGRATIONS + "/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"connected\",\"configuredAt\":\"2026-09-26T10:00:00Z\"}"));
        call("integrations", "GET", INTEGRATIONS, get(INTEGRATIONS));
        call("operational-events-accepted", "POST", EVENTS, post(EVENTS).contentType(MediaType.APPLICATION_JSON)
                .content("{\"events\":[{\"id\":\"" + UUID.randomUUID() + "\",\"correlationId\":\""
                        + UUID.randomUUID() + "\",\"occurredAt\":\"" + Instant.now().truncatedTo(ChronoUnit.MILLIS)
                        + "\",\"release\":\"0.2.199\",\"domain\":\"database\",\"operation\":\"read\","
                        + "\"outcome\":\"ok\",\"reason\":\"ok\",\"attempt\":1,\"durationMs\":120,"
                        + "\"recoveryMs\":null,\"freshness\":\"fresh\"}]}"));
    }

    /** Enough failures for a recommendation, so every part of the insights appears in the fixture. */
    @Test
    void activityEventsAndInsightsKeepTheirContractShape() throws Exception {
        var events = new ArrayList<>(ActivityEventJson.threeSessions());
        var session = UUID.randomUUID();
        events.add(ActivityEventJson.event(session, 1, "error", "tasks", "save_failed").with("surface", "tasks")
                .with("outcome", "failure").with("errorCode", "tasks_save"));
        events.add(ActivityEventJson.event(session, 2, "outcome", "calendar", "sync_finished")
                .with("surface", "calendar").with("outcome", "failure").with("errorCode", "calendar_sync"));

        call("activity-events-accepted", "POST", ACTIVITY_EVENTS, post(ACTIVITY_EVENTS)
                .contentType(MediaType.APPLICATION_JSON).content(ActivityEventJson.batch(events)));
        call("activity-events-invalid", "POST", ACTIVITY_EVENTS, post(ACTIVITY_EVENTS)
                .contentType(MediaType.APPLICATION_JSON).content(ActivityEventJson.batch(List.of(
                        ActivityEventJson.event(UUID.randomUUID(), 1, "action", "tasks", "created")
                                .with("metadata", Map.of("note", "private text"))))));
        call("activity-insights", "GET", ACTIVITY_INSIGHTS, get(ACTIVITY_INSIGHTS).param("rangeDays", "30"));
    }

    @Test
    void aRateLimitedRequestKeepsItsContractShape() throws Exception {
        var limited = UUID.randomUUID();
        var response = mockMvc.perform(get(SETTINGS).with(jwt().jwt(token -> token.subject(limited.toString()))))
                .andReturn().getResponse();
        for (int request = 0; request < 200 && response.getStatus() != 429; request++) {
            response = mockMvc.perform(get(SETTINGS).with(jwt().jwt(token -> token.subject(limited.toString()))))
                    .andReturn().getResponse();
        }
        CONTRACT.check("rate-limited", "GET", SETTINGS, response.getStatus(), response.getContentAsString());
    }

    @Test
    void perKeyViewPreferencesKeepTheSharedConsumerContract() throws Exception {
        var path = "/api/profile/v1/view-preferences";
        call("view-preference-updated", "PUT", path + "/{key}", put(path + "/prayerChallenge.period")
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"day\"}"));
        mockMvc.perform(put(path + "/dashboard.learnIndividualSteps").contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"true\"}").with(jwt().jwt(token -> token.subject(user.toString()))))
                .andExpect(status().isOk());
        call("view-preferences", "GET", path, get(path));
    }
}
