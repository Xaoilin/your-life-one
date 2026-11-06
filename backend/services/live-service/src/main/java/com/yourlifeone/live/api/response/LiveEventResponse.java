package com.yourlifeone.live.api.response;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * One event on a user's stream: something changed and the tab should reload that part of the app, or a
 * prayer reminder is due.
 *
 * @param type what happened, such as {@code lifestyle.save-item} or {@code prayer.reminder}
 * @param domain the part of the app to reload, such as {@code lifestyle}
 * @param data details for events that carry them (a prayer reminder); null otherwise
 */
public record LiveEventResponse(String type, String domain, Instant at, JsonNode data) {
}
