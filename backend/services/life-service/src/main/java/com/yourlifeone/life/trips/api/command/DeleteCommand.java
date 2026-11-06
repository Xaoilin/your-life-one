package com.yourlifeone.life.trips.api.command;

/** Deletes one trip or one part of a trip; the queued payload carries the ID from the path. */
public record DeleteCommand(String id) {
}
