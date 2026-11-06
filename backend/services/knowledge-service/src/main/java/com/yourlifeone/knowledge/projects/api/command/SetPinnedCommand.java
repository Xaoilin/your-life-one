package com.yourlifeone.knowledge.projects.api.command;

import com.yourlifeone.knowledge.projects.api.request.PinnedRequest;

/** Pins or unpins one project. The queued payload carries the ID from the request path. */
public record SetPinnedCommand(String id, PinnedRequest pinned) {
}
