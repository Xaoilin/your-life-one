package com.yourlifeone.life.health.api.command;

/** Deletes one entry. The queued payload carries the ID from the request path. */
public record DeleteFastFoodEntryCommand(String id) {
}
