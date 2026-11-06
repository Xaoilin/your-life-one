package com.yourlifeone.knowledge.lifestyle.api.response;

import java.time.Instant;
import java.util.List;

public record LifestyleItemResponse(
        String id,
        String type,
        String title,
        String notes,
        String status,
        List<String> sources,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
