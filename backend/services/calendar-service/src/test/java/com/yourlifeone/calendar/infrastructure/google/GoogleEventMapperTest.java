package com.yourlifeone.calendar.infrastructure.google;

import com.yourlifeone.calendar.application.model.GoogleEventDraft;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventResource;
import com.yourlifeone.calendar.infrastructure.google.dto.GoogleEventTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GoogleEventMapperTest {
    private final GoogleEventMapper mapper = new GoogleEventMapper();

    @Test
    void googleAllDayEndsAreExclusiveSoTheLastDayIsTheDayBefore() {
        var event = mapper.toEvent(new GoogleEventResource("e1", "confirmed", "Holiday", null, " ",
                new GoogleEventTime("2026-10-01", null, null), new GoogleEventTime("2026-10-03", null, null)));

        assertEquals("Holiday", event.title());
        assertEquals("", event.description());
        assertNull(event.location());
        assertEquals("2026-10-01", event.timing().startValue());
        assertEquals("2026-10-02", event.timing().endValue());
    }

    @Test
    void timedEventsKeepTheirInstantsAndUntitledEventsGetATitle() {
        var event = mapper.toEvent(new GoogleEventResource("e2", "confirmed", null, "Notes", "Office",
                new GoogleEventTime(null, "2026-10-01T09:00:00+01:00", "Europe/London"),
                new GoogleEventTime(null, "2026-10-01T10:00:00+01:00", "Europe/London")));

        assertEquals("(No title)", event.title());
        assertEquals(Instant.parse("2026-10-01T08:00:00Z"), event.timing().startsAt());
        assertEquals(Instant.parse("2026-10-01T09:00:00Z"), event.timing().endsAt());
    }

    @Test
    void writesAllDayEventsAsDatesAndTimedEventsInTheAppTimeZone() {
        var allDay = mapper.toBody(GoogleEventDraft.builder().title(" Trip ").description(null)
                .timing(EventTiming.parse(true, "2026-10-01", "2026-10-02")).timeZone("Europe/London").build());
        var timed = mapper.toBody(GoogleEventDraft.builder().title("Call").description("x").location("Home")
                .timing(EventTiming.parse(false, "2026-10-01T08:00:00Z", "2026-10-01T09:00:00Z"))
                .timeZone("Europe/London").build());

        assertEquals("Trip", allDay.summary());
        assertEquals("", allDay.description());
        assertEquals(new GoogleEventTime("2026-10-01", null, null), allDay.start());
        assertEquals(new GoogleEventTime("2026-10-03", null, null), allDay.end());
        assertEquals(new GoogleEventTime(null, "2026-10-01T08:00:00Z", "Europe/London"), timed.start());
        assertEquals("Home", timed.location());
    }
}
