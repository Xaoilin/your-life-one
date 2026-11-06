package com.yourlifeone.knowledge.knowledge.api.command;

import com.yourlifeone.knowledge.knowledge.api.request.EntryRequest;

/** Creates or replaces one entry. The queued payload carries the ID from the request path. */
public record SaveEntryCommand(String id, EntryRequest entry) {
}
