package com.yourlifeone.calendar.application.model;

import com.yourlifeone.calendar.domain.EventTiming;
import lombok.Builder;

/**
 * What the user sets on an event.
 *
 * @param timeZone IANA zone Google shows a timed event in; null uses UTC
 */
@Builder
public record EventDetails(
        String sourceId, String title, String description, String location, EventTiming timing, String timeZone) {
}
