package com.yourlifeone.life.health.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * One fast-food meal in the health journal.
 *
 * @param order what was eaten; null when not recorded
 * @param symptoms how the user felt afterwards
 */
@Builder(toBuilder = true)
public record FastFoodEntry(
        String id,
        String venue,
        LocalDate date,
        String order,
        String rating,
        List<String> symptoms,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
