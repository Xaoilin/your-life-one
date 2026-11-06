package com.yourlifeone.life.trips.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record ItineraryItemResponse(String id, String tripId, String legId, LocalDate date, String title,
        String startTime, String endTime, String location, String notes, int sortOrder, Instant createdAt,
        Instant updatedAt) {
}
