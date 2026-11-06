package com.yourlifeone.calendar.infrastructure.google;

import com.yourlifeone.calendar.application.model.GoogleCalendar;
import com.yourlifeone.calendar.application.model.GoogleEvent;
import com.yourlifeone.calendar.application.model.GoogleEventDraft;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleCalendarListEntry;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventBody;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventResource;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/** Converts between Google Calendar API resources and Your Life One's calendar model. */
@Component
public class GoogleEventMapper {
    private static final String UNTITLED = "(No title)";
    private static final int MAX_TITLE_LENGTH = 500;

    public GoogleCalendar toCalendar(GoogleCalendarListEntry entry) {
        return GoogleCalendar.builder()
                .id(entry.id())
                .summary(entry.summaryOverride() == null ? entry.summary() : entry.summaryOverride())
                .backgroundColor(entry.backgroundColor())
                .primary(Boolean.TRUE.equals(entry.primary()))
                .accessRole(entry.accessRole())
                .selected(entry.selected())
                .build();
    }

    /** Google all-day events end on the day after their last day, which is how Your Life One stores them. */
    public GoogleEvent toEvent(GoogleEventResource resource) {
        return GoogleEvent.builder()
                .id(resource.id())
                .title(titleOf(resource.summary()))
                .description(resource.description() == null ? "" : resource.description())
                .location(resource.location() == null || resource.location().isBlank() ? null : resource.location())
                .timing(timingOf(resource.start(), resource.end()))
                .build();
    }

    public GoogleEventBody toBody(GoogleEventDraft draft) {
        var timing = draft.timing();
        return GoogleEventBody.builder()
                .summary(draft.title().trim())
                // Empty strings, not nulls, so a change can clear them.
                .description(draft.description() == null ? "" : draft.description())
                .location(draft.location() == null ? "" : draft.location().trim())
                .start(timing.allDay()
                        ? new GoogleEventTime(timing.startDate().toString(), null, null)
                        : new GoogleEventTime(null, timing.startsAt().toString(), draft.timeZone()))
                .end(timing.allDay()
                        ? new GoogleEventTime(timing.exclusiveEndDate().toString(), null, null)
                        : new GoogleEventTime(null, timing.endsAt().toString(), draft.timeZone()))
                .build();
    }

    private static EventTiming timingOf(GoogleEventTime start, GoogleEventTime end) {
        if (start.dateTime() == null) {
            var startDate = LocalDate.parse(start.date());
            var exclusiveEnd = end == null || end.date() == null ? startDate.plusDays(1) : LocalDate.parse(end.date());
            return EventTiming.allDay(startDate, exclusiveEnd.isAfter(startDate) ? exclusiveEnd.minusDays(1) : startDate);
        }
        var startsAt = OffsetDateTime.parse(start.dateTime()).toInstant();
        var endsAt = end == null || end.dateTime() == null ? startsAt : OffsetDateTime.parse(end.dateTime()).toInstant();
        return EventTiming.timed(startsAt, endsAt.isBefore(startsAt) ? startsAt : endsAt);
    }

    private static String titleOf(String summary) {
        if (summary == null || summary.isBlank()) return UNTITLED;
        return summary.length() > MAX_TITLE_LENGTH ? summary.substring(0, MAX_TITLE_LENGTH) : summary;
    }
}
