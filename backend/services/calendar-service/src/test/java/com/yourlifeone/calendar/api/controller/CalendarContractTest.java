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
import com.yourlifeone.commonservice.testing.ContractFixtures;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Provider side of the contract with the React app: responses keep the shape of their fixtures in
 * {@code contracts/calendar-service}, which the app also uses as mocks.
 */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class CalendarContractTest {
    private static final ContractFixtures CONTRACT = new ContractFixtures("contracts/calendar-service");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String V1 = "/api/calendar/v1";
    private static final String EMAIL = "yourlife@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleOAuthApi oauth;

    @MockitoBean
    private GoogleCalendarApi google;

    private final UUID user = UUID.randomUUID();

    private JsonNode call(String name, String method, String path, MockHttpServletRequestBuilder request)
            throws Exception {
        var response = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(token -> token.subject(user.toString()))))
                .andReturn().getResponse();
        CONTRACT.check(name, method, path, response.getStatus(), response.getContentAsString());
        return response.getContentAsString().isEmpty() ? null : JSON.readTree(response.getContentAsString());
    }

    @Test
    void everyResponseTheReactAppUsesKeepsItsContractShape() throws Exception {
        var grant = GoogleTokenGrant.builder().accessToken("google-token").refreshToken("refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(oauth.exchangeCode(anyString(), anyString())).thenReturn(grant);
        when(oauth.account("google-token")).thenReturn(new ConnectedGoogleAccount(EMAIL, "YourLife"));
        when(oauth.refresh("refresh-token")).thenReturn(grant);
        when(google.calendars("google-token")).thenReturn(List.of(GoogleCalendar.builder().id(EMAIL)
                .summary("YourLife").primary(true).accessRole("owner").backgroundColor("#4f5bff").build()));
        when(google.events(eq("google-token"), eq(EMAIL), any(), any())).thenReturn(List.of(GoogleEvent.builder()
                .id("g1").title("Dentist").description("Check-up").location("High Street")
                .timing(EventTiming.parse(false, "2026-10-01T09:00:00Z", "2026-10-01T10:00:00Z")).build()));
        when(google.create(eq("google-token"), eq(EMAIL), any())).thenReturn(GoogleEvent.builder()
                .id("g2").title("Lunch").description("").timing(EventTiming.parse(true, "2026-10-05", "2026-10-05"))
                .build());

        var connected = call("account-connected", "POST", V1 + "/accounts/google", post(V1 + "/accounts/google")
                .content("{\"code\":\"4/0Ab\",\"redirectUri\":\"https://your-life-one.example\"}"));
        var local = call("account-created", "POST", V1 + "/accounts", post(V1 + "/accounts")
                .content("{\"name\":\"Home\",\"paletteIndex\":3}"));
        call("account-updated", "PATCH", V1 + "/accounts/{accountId}",
                patch(V1 + "/accounts/" + local.path("id").asString()).content("{\"primary\":true}"));
        call("sync", "POST", V1 + "/sync", post(V1 + "/sync"));
        var calendar = call("calendar", "GET", V1 + "/calendar?from=2026-09-01&to=2026-12-31",
                get(V1 + "/calendar?from=2026-09-01&to=2026-12-31"));
        call("events", "GET", V1 + "/events?from=2026-10-01&to=2026-10-31",
                get(V1 + "/events?from=2026-10-01&to=2026-10-31"));

        String googleSource = null;
        String localSource = null;
        for (var source : calendar.path("sources")) {
            if (source.path("googleCalendarId").isNull()) localSource = source.path("id").asString();
            else googleSource = source.path("id").asString();
        }
        call("source-created", "POST", V1 + "/sources", post(V1 + "/sources").content("""
                {"accountId":"%s","name":"Family","color":"#22aa66"}""".formatted(local.path("id").asString())));
        call("source-updated", "PATCH", V1 + "/sources/{sourceId}",
                patch(V1 + "/sources/" + googleSource).content("{\"visible\":false,\"color\":\"#ff8800\"}"));
        call("event-created", "POST", V1 + "/events", post(V1 + "/events").content("""
                {"sourceId":"%s","title":"Lunch","allDay":true,"start":"2026-10-05","end":"2026-10-05",
                 "timeZone":"Europe/London"}""".formatted(googleSource)));
        var localEvent = call("event-created-local", "POST", V1 + "/events", post(V1 + "/events").content("""
                {"sourceId":"%s","title":"Call","description":"","location":"Home","allDay":false,
                 "start":"2026-10-06T09:00:00Z","end":"2026-10-06T09:30:00Z"}""".formatted(localSource)));
        call("event-updated", "PUT", V1 + "/events/{eventId}",
                put(V1 + "/events/" + localEvent.path("id").asString()).content("""
                        {"sourceId":"%s","title":"Call Alex","allDay":false,"start":"2026-10-06T10:00:00Z",
                         "end":"2026-10-06T10:30:00Z"}""".formatted(localSource)));
        call("event-invalid", "POST", V1 + "/events", post(V1 + "/events").content("""
                {"sourceId":"%s","title":"Call","allDay":false,"start":"tomorrow","end":"later"}"""
                .formatted(localSource)));

        when(oauth.refresh("refresh-token")).thenThrow(new GoogleReconnectRequiredException(
                AuthStatus.NEEDS_RECONNECT, "Google refresh token expired or was revoked. Reconnect this account."));
        when(google.calendars("google-token")).thenThrow(new GoogleReconnectRequiredException(
                AuthStatus.NEEDS_RECONNECT, "Google access expired or was revoked. Reconnect this account."));
        call("sync-needs-reconnect", "POST", V1 + "/sync", post(V1 + "/sync"));
        call("google-reconnect-required", "POST", V1 + "/events", post(V1 + "/events").content("""
                {"sourceId":"%s","title":"Lunch","allDay":true,"start":"2026-10-07","end":"2026-10-07"}"""
                .formatted(googleSource)));
    }
}
