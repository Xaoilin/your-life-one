package com.yourlifeone.calendar.api.mapper;

import com.yourlifeone.calendar.api.request.EventRequest;
import com.yourlifeone.calendar.application.model.EventDetails;
import com.yourlifeone.calendar.domain.EventTiming;
import com.yourlifeone.calendar.domain.InvalidEventTimingException;
import com.yourlifeone.commonservice.web.ApiException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/** Reads an event request into the application model; unreadable times are a 400. */
@Component
public class EventRequestMapper {
    public EventDetails toDetails(EventRequest request) {
        return EventDetails.builder()
                .sourceId(request.sourceId())
                .title(request.title().trim())
                .description(request.description() == null ? "" : request.description())
                .location(request.location())
                .timing(timingOf(request))
                .timeZone(zoneOf(request.timeZone()))
                .build();
    }

    private static EventTiming timingOf(EventRequest request) {
        try {
            return EventTiming.parse(request.allDay(), request.start().trim(), request.end().trim());
        } catch (InvalidEventTimingException exception) {
            throw ApiException.badRequest("invalid_event_time", exception.getMessage());
        }
    }

    /** A named IANA region such as Europe/London; null when absent. */
    private static String zoneOf(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) return null;
        try {
            var zone = ZoneId.of(timeZone.trim());
            if (!(zone instanceof ZoneOffset) && ZoneId.getAvailableZoneIds().contains(zone.getId())) {
                return zone.getId();
            }
        } catch (DateTimeException exception) {
            // Reported below as a bounded validation error.
        }
        throw ApiException.badRequest("invalid_time_zone", "timeZone must be an IANA time zone such as Europe/London.");
    }
}
