package com.yourlifeone.knowledge.lifestyle.api.command;

import com.yourlifeone.knowledge.lifestyle.api.request.MoveItemRequest;

/** Moves one item. The queued payload carries the ID from the request path. */
public record MoveItemCommand(String id, MoveItemRequest move) {
}
