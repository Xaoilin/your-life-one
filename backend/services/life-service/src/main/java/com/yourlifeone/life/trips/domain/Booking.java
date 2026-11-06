package com.yourlifeone.life.trips.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/**
 * A transport or stay booking. Transport uses mode, route and local depart/arrive times
 * (YYYY-MM-DDTHH:MM); a stay uses property, place and check-in/out dates.
 *
 * @param budgetAmount cost in minor units; null when not costed yet
 */
@Builder(toBuilder = true)
public record Booking(
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
    public static final String TRANSPORT = "transport";
    public static final String STAY = "stay";
}
