package com.yourlifeone.calendar.infrastructure.google;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.exception.GoogleRefusedChangeException;
import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.application.model.GoogleEventDraft;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.EventTiming;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleCalendarRestApiTest {
    private static final String BASE = "https://google.test/calendar/v3";
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-01T00:00:00Z");

    private MockRestServiceServer server;
    private GoogleCalendarRestApi api;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        api = new GoogleCalendarRestApi(builder.build(), new GoogleEventMapper());
    }

    @Test
    void followsEveryPageAndDropsCancelledEvents() {
        var events = BASE + "/calendars/team%23x%40group.calendar.google.com/events?singleEvents=true"
                + "&timeMin=2026-09-01T00:00:00Z&timeMax=2026-10-01T00:00:00Z&maxResults=2500";
        server.expect(requestTo(events)).andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("""
                        {"items":[{"id":"a","summary":"One","start":{"date":"2026-09-02"},"end":{"date":"2026-09-03"}},
                                  {"id":"b","status":"cancelled","start":{"date":"2026-09-02"},"end":{"date":"2026-09-03"}}],
                         "nextPageToken":"p2"}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo(events + "&pageToken=p2")).andRespond(withSuccess("""
                {"items":[{"id":"c","summary":"Two","start":{"dateTime":"2026-09-04T09:00:00Z"},
                           "end":{"dateTime":"2026-09-04T10:00:00Z"}}]}""", MediaType.APPLICATION_JSON));

        var fetched = api.events("token", "team#x@group.calendar.google.com", FROM, TO);

        assertEquals(2, fetched.size());
        assertEquals("a", fetched.get(0).id());
        assertEquals("c", fetched.get(1).id());
    }

    @Test
    void anExpiredGoogleCredentialAsksForReconnectionNotSignIn() {
        server.expect(requestTo(BASE + "/users/me/calendarList?minAccessRole=reader"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        var exception = assertThrows(GoogleReconnectRequiredException.class, () -> api.calendars("token"));

        assertEquals(AuthStatus.NEEDS_RECONNECT, exception.authStatus());
        assertEquals(HttpStatus.CONFLICT, exception.status());
    }

    @Test
    void rateLimitsAreRetryableAndRefusalsAreNot() {
        server.expect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .body("{\"error\":{\"errors\":[{\"reason\":\"rateLimitExceeded\"}]}}"));
        server.expect(method(HttpMethod.PATCH)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .body("{\"error\":{\"errors\":[{\"reason\":\"requiredAccessLevel\"}]}}"));

        var draft = GoogleEventDraft.builder().title("Call").timeZone("UTC")
                .timing(EventTiming.parse(true, "2026-09-02", "2026-09-02")).build();

        assertThrows(GoogleUnavailableException.class, () -> api.create("token", "cal", draft));
        assertThrows(GoogleRefusedChangeException.class, () -> api.update("token", "cal", "e1", draft));
    }

    @Test
    void deletingAnEventGoogleNoLongerHasSucceeds() {
        server.expect(requestTo(BASE + "/calendars/cal/events/e1")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.GONE));

        assertDoesNotThrow(() -> api.delete("token", "cal", "e1"));
    }
}
