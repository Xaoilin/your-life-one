package com.yourlifeone.calendar.infrastructure.google;

import com.yourlifeone.calendar.application.exception.GoogleReconnectRequiredException;
import com.yourlifeone.calendar.application.exception.GoogleRefusedChangeException;
import com.yourlifeone.calendar.application.exception.GoogleUnavailableException;
import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.model.GoogleEvent;
import com.yourlifeone.calendar.application.model.GoogleEventDraft;
import com.yourlifeone.calendar.application.port.GoogleCalendarApi;
import com.yourlifeone.calendar.domain.AuthStatus;
import com.yourlifeone.calendar.domain.InvalidEventTimingException;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleCalendarListResponse;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventResource;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventsResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Google Calendar API v3 over HTTPS (https://developers.google.com/workspace/calendar/api/v3/reference). */
public class GoogleCalendarRestApi implements GoogleCalendarApi {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarRestApi.class);
    private static final String EVENTS_PAGE_SIZE = "2500";

    private final RestClient google;
    private final GoogleEventMapper mapper;

    /** @param google a client whose base URL is the Calendar API root */
    public GoogleCalendarRestApi(RestClient google, GoogleEventMapper mapper) {
        this.google = google;
        this.mapper = mapper;
    }

    @Override
    public List<GoogleCalendar> calendars(String accessToken) {
        var calendars = new ArrayList<GoogleCalendar>();
        String pageToken = null;
        do {
            var page = pageToken;
            var response = call("list calendars", () -> google.get()
                    .uri(uri -> uri.path("/users/me/calendarList")
                            .queryParam("minAccessRole", "reader")
                            .queryParamIfPresent("pageToken", Optional.ofNullable(page))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(GoogleCalendarListResponse.class));
            if (response == null) break;
            if (response.items() != null) response.items().forEach(entry -> calendars.add(mapper.toCalendar(entry)));
            pageToken = response.nextPageToken();
        } while (pageToken != null);
        return calendars;
    }

    @Override
    public List<GoogleEvent> events(String accessToken, String calendarId, Instant from, Instant to) {
        var events = new ArrayList<GoogleEvent>();
        String pageToken = null;
        do {
            var page = pageToken;
            var response = call("list events", () -> google.get()
                    .uri(uri -> uri.path("/calendars/{calendarId}/events")
                            .queryParam("singleEvents", "true")
                            .queryParam("timeMin", from.toString())
                            .queryParam("timeMax", to.toString())
                            .queryParam("maxResults", EVENTS_PAGE_SIZE)
                            .queryParamIfPresent("pageToken", Optional.ofNullable(page))
                            .build(calendarId))
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(GoogleEventsResponse.class));
            if (response == null) break;
            if (response.items() != null) {
                response.items().stream()
                        .filter(item -> !"cancelled".equals(item.status()))
                        .forEach(item -> toEvent(item, calendarId, events));
            }
            pageToken = response.nextPageToken();
        } while (pageToken != null);
        return events;
    }

    @Override
    public GoogleEvent create(String accessToken, String calendarId, GoogleEventDraft draft) {
        var created = call("create event", () -> google.post()
                .uri("/calendars/{calendarId}/events", calendarId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .body(mapper.toBody(draft))
                .retrieve()
                .body(GoogleEventResource.class));
        return mapper.toEvent(created);
    }

    @Override
    public GoogleEvent update(String accessToken, String calendarId, String eventId, GoogleEventDraft draft) {
        try {
            var updated = call("update event", () -> google.patch()
                    .uri("/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .body(mapper.toBody(draft))
                    .retrieve()
                    .body(GoogleEventResource.class));
            return mapper.toEvent(updated);
        } catch (EventGoneException exception) {
            throw new GoogleRefusedChangeException("This event no longer exists in Google Calendar; sync to refresh.");
        }
    }

    @Override
    public void delete(String accessToken, String calendarId, String eventId) {
        try {
            call("delete event", () -> google.delete()
                    .uri("/calendars/{calendarId}/events/{eventId}", calendarId, eventId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .toBodilessEntity());
        } catch (EventGoneException exception) {
            // Already deleted in Google: the outcome the user asked for.
        }
    }

    private void toEvent(GoogleEventResource item, String calendarId, List<GoogleEvent> events) {
        try {
            events.add(mapper.toEvent(item));
        } catch (DateTimeParseException | InvalidEventTimingException | NullPointerException exception) {
            logger.warn("Skipped a Google event with unreadable times: calendarId={} eventId={}", calendarId, item.id());
        }
    }

    /** Translates Google's failures: 401 means the credential is dead, never that the Your Life One user is. */
    private static <T> T call(String operation, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new GoogleReconnectRequiredException(AuthStatus.NEEDS_RECONNECT,
                    "Google access expired or was revoked. Reconnect this account.");
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Gone exception) {
            throw new EventGoneException();
        } catch (HttpClientErrorException.TooManyRequests exception) {
            throw new GoogleUnavailableException("Google rate limited " + operation, exception);
        } catch (HttpClientErrorException.Forbidden exception) {
            if (exception.getResponseBodyAsString().contains("RateLimitExceeded")
                    || exception.getResponseBodyAsString().contains("rateLimitExceeded")) {
                throw new GoogleUnavailableException("Google rate limited " + operation, exception);
            }
            throw new GoogleRefusedChangeException("Google refused this change; the calendar may be read-only.");
        } catch (HttpClientErrorException exception) {
            logger.warn("Google refused {}: status={}", operation, exception.getStatusCode().value());
            throw new GoogleRefusedChangeException("Google refused this request.");
        } catch (RestClientException exception) {
            throw new GoogleUnavailableException("Google " + operation + " failed: " + exception.getMessage(), exception);
        }
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    /** Google no longer has the calendar or event (404 or 410). */
    private static final class EventGoneException extends GoogleRefusedChangeException {
        EventGoneException() {
            super("Google no longer has this calendar or event; sync to refresh.");
        }
    }
}
