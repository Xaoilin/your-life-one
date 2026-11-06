package com.yourlifeone.knowledge.lifestyle.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Something to avoid or to practise, in its type's column of the lifestyle tracker.
 *
 * @param type the column, e.g. {@code haram} or {@code wajib-both}
 * @param status progress within the type's family, e.g. {@code avoiding} or {@code practicing}
 * @param sources Quran or hadith references, e.g. links
 * @param sortOrder position in the column
 */
@Builder(toBuilder = true)
public record LifestyleItem(
        String id,
        String type,
        String title,
        String notes,
        String status,
        List<String> sources,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
