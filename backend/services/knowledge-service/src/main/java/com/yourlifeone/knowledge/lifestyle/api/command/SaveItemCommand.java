package com.yourlifeone.knowledge.lifestyle.api.command;

import com.yourlifeone.knowledge.lifestyle.api.request.LifestyleItemRequest;

/** Creates or replaces one item. The queued payload carries the ID from the request path. */
public record SaveItemCommand(String id, LifestyleItemRequest item) {
}
