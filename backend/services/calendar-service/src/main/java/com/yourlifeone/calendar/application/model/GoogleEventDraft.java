package com.yourlifeone.calendar.application.model;

import com.yourlifeone.calendar.domain.EventTiming;
import lombok.Builder;

/**
 * An event to create or change in Google Calendar.
 *
 * @param timeZone IANA zone Google shows a timed event in
 */
@Builder
public record GoogleEventDraft(String title, String description, String location, EventTiming timing, String timeZone) {
}
