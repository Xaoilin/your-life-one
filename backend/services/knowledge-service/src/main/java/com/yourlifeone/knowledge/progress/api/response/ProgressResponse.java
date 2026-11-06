package com.yourlifeone.knowledge.progress.api.response;

import lombok.Builder;

/** Counts of the user's knowledge entries, topics and lifestyle items. */
@Builder
public record ProgressResponse(long entries, long topics, long haramMastered, long halalConsistent,
        long lifestyleItems) {
}
