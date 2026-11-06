package com.yourlifeone.knowledge.knowledge.api.response;

import java.time.Instant;

public record TopicResponse(
        String id,
        String name,
        String description,
        String icon,
        String color,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
