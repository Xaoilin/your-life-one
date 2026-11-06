package com.yourlifeone.life.inventory.api.command;

/** Archives one item; the queued payload carries the ID from the request path. */
public record ArchiveItemCommand(String id) {
}
