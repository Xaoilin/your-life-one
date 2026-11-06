package com.yourlifeone.life.health.api.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FastFoodEntryResponse(
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
