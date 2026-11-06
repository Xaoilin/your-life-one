package com.yourlifeone.life.trips.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record TripLegResponse(String id, String tripId, String country, String city, LocalDate startDate,
        LocalDate endDate, int sortOrder, Instant createdAt, Instant updatedAt) {
}
