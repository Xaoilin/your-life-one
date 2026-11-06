package com.yourlifeone.life.jobs.api.command;

import com.yourlifeone.life.jobs.api.request.HistoryEntryRequest;

/** Appends evidence to one application; the queued payload carries the ID from the request path. */
public record AddHistoryCommand(String applicationId, HistoryEntryRequest entry) {
}
