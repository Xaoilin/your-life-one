package com.yourlifeone.life.trips.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        String id,
        String tripId,
        String legId,
        String kind,
        String title,
        String mode,
        String fromLabel,
        String toLabel,
        String departAt,
        String arriveAt,
        String propertyName,
        String address,
        String city,
        String country,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long budgetAmount,
        String budgetStatus,
        LocalDate budgetDate,
        String provider,
        String confirmationCode,
        String link,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
