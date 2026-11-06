package com.yourlifeone.calendar.domain;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTimingTest {
    @Test
    void allDayEventsKeepTheirInclusiveDatesAndStoreAnExclusiveUtcEnd() {
        var timing = EventTiming.parse(true, "2026-09-26", "2026-09-27");

        assertEquals(Instant.parse("2026-09-26T00:00:00Z"), timing.startsAt());
        assertEquals(Instant.parse("2026-09-28T00:00:00Z"), timing.endsAt());
        assertEquals("2026-09-26", timing.startValue());
        assertEquals("2026-09-27", timing.endValue());
        assertEquals(LocalDate.parse("2026-09-28"), timing.exclusiveEndDate());
    }

    @Test
    void timedEventsAreInstantsWhateverOffsetTheyArriveIn() {
        var timing = EventTiming.parse(false, "2026-09-26T10:00:00+01:00", "2026-09-26T10:30:00Z");

        assertEquals("2026-09-26T09:00:00Z", timing.startValue());
        assertEquals("2026-09-26T10:30:00Z", timing.endValue());
    }

    @Test
    void refusesUnreadableOrBackwardsTimes() {
        assertThrows(InvalidEventTimingException.class, () -> EventTiming.parse(false, "2026-09-26", "2026-09-26"));
        assertThrows(InvalidEventTimingException.class, () -> EventTiming.parse(true, "2026-09-27", "2026-09-25"));
        assertThrows(InvalidEventTimingException.class,
                () -> EventTiming.parse(false, "2026-09-26T11:00:00Z", "2026-09-26T10:00:00Z"));
    }
}
