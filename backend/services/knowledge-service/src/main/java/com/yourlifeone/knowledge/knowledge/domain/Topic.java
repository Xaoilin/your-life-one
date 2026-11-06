package com.yourlifeone.knowledge.knowledge.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A knowledge-base topic that groups entries.
 *
 * @param sortOrder position among the user's topics; null on a new topic places it last
 */
@Builder(toBuilder = true)
public record Topic(
        String id,
        String name,
        String description,
        String icon,
        String color,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
