package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.request.EventRequest;

/** Changes one event. The queued payload carries the ID from the request path. */
public record UpdateEventCommand(String eventId, EventRequest event) {
}
