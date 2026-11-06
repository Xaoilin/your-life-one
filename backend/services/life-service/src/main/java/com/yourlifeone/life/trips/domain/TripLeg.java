package com.yourlifeone.life.trips.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/** One stop on a trip's route; {@code sortOrder} is its place in the route, 0 first. */
@Builder(toBuilder = true)
public record TripLeg(
        String id,
        String tripId,
        String country,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
