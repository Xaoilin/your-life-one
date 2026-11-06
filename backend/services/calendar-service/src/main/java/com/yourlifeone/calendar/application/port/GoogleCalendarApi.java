package com.yourlifeone.calendar.application.port;

import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.model.GoogleEvent;
import com.yourlifeone.calendar.application.model.GoogleEventDraft;
import java.time.Instant;
import java.util.List;

/** Google Calendar API v3, called with an account's access token. */
public interface GoogleCalendarApi {
    List<GoogleCalendar> calendars(String accessToken);

    /** Single (expanded) events overlapping {@code [from, to)}, without cancelled ones. */
    List<GoogleEvent> events(String accessToken, String calendarId, Instant from, Instant to);

    GoogleEvent create(String accessToken, String calendarId, GoogleEventDraft draft);

    GoogleEvent update(String accessToken, String calendarId, String eventId, GoogleEventDraft draft);

    /** Succeeds when the event is already gone. */
    void delete(String accessToken, String calendarId, String eventId);
}
