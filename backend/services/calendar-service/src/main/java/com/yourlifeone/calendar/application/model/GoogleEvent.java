package com.yourlifeone.calendar.application.model;

import com.yourlifeone.calendar.domain.EventTiming;
import lombok.Builder;

/** A Google Calendar event, already converted to Your Life One's timing. */
@Builder
public record GoogleEvent(String id, String title, String description, String location, EventTiming timing) {
}
