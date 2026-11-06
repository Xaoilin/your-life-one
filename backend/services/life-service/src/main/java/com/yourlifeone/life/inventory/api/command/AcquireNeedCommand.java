package com.yourlifeone.life.inventory.api.command;

/** Marks one need acquired and adds it to stock; the queued payload carries the ID from the path. */
public record AcquireNeedCommand(String needId, String newItemId) {
}
