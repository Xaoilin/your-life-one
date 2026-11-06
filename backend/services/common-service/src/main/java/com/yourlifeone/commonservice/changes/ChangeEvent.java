package com.yourlifeone.commonservice.changes;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import tools.jackson.databind.JsonNode;

/**
 * Something changed in a user's data, published on the {@code yourlife.changes} exchange after the change
 * is committed. The live-update gateway forwards it to the user's open tabs, which reload that domain.
 *
 * @param userId whose data changed; used for routing and never sent to a browser
 * @param type what changed, a write command type such as {@code lifestyle.save-item}; also the routing key
 * @param domain the part of the app to reload: the type up to its first dot, such as {@code lifestyle}
 * @param data optional details a tab needs without reloading (a prayer reminder), or null
 */
@Builder
public record ChangeEvent(UUID userId, String type, String domain, Instant at, JsonNode data) {
    /** An event of this type now, with its domain taken from the type. */
    public static ChangeEvent of(UUID userId, String type, Instant at, JsonNode data) {
        return new ChangeEvent(userId, type, domainOf(type), at, data);
    }

    static String domainOf(String type) {
        var dot = type.indexOf('.');
        return dot < 0 ? type : type.substring(0, dot);
    }
}
