package com.yourlifeone.calendar.api.command;

/** Deletes one event. The queued payload carries the ID from the request path. */
public record DeleteEventCommand(String eventId) {
}
