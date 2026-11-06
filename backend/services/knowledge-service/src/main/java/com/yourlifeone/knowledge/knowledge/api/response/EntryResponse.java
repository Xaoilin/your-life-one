package com.yourlifeone.knowledge.knowledge.api.response;

import java.time.Instant;
import java.util.List;

public record EntryResponse(
        String id,
        String topicId,
        String title,
        String content,
        List<SourceResponse> sources,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt) {
}
