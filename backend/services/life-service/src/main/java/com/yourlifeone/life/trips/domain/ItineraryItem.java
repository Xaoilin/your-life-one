package com.yourlifeone.life.trips.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/**
 * A plan on one day of a leg.
 *
 * @param startTime local time HH:MM; null for an untimed plan
 */
@Builder(toBuilder = true)
public record ItineraryItem(
        String id,
        String tripId,
        String legId,
        LocalDate date,
        String title,
        String startTime,
        String endTime,
        String location,
        String notes,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
