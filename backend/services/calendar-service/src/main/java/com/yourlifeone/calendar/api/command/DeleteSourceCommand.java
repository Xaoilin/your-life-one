package com.yourlifeone.calendar.api.command;

/** Deletes one local calendar and its events. The queued payload carries the ID from the request path. */
public record DeleteSourceCommand(String sourceId) {
}
