package com.yourlifeone.calendar.api.command;

import com.yourlifeone.calendar.api.request.UpdateSourceRequest;

/** Changes one calendar. The queued payload carries the ID from the request path. */
public record UpdateSourceCommand(String sourceId, UpdateSourceRequest changes) {
}
