package com.yourlifeone.life.inventory.api.command;

import com.yourlifeone.life.inventory.api.request.ItemRequest;

/** Creates or replaces one item; the queued payload carries the ID from the request path. */
public record SaveItemCommand(String id, ItemRequest item) {
}
