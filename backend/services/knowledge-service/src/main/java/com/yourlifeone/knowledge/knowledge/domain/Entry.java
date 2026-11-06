package com.yourlifeone.knowledge.knowledge.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/** A note in the knowledge base, inside one topic. */
@Builder(toBuilder = true)
public record Entry(
        String id,
        String topicId,
        String title,
        String content,
        List<KnowledgeSource> sources,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt) {
}
