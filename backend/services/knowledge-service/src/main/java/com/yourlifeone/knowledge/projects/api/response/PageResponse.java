package com.yourlifeone.knowledge.projects.api.response;

import java.time.Instant;

public record PageResponse(
        String id,
        String projectId,
        String title,
        String content,
        boolean isOverview,
        Instant createdAt,
        Instant updatedAt) {
}
