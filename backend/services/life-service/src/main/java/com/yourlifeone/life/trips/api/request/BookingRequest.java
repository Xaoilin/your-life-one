package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * One booking; the ID is in the path (in a plan, {@code id} names it and {@code legId} a plan leg).
 * Transport fields apply to {@code kind=transport}, stay fields to {@code kind=stay}.
 */
public record BookingRequest(
        @Size(max = 256) String id,
        @NotBlank @Size(max = 256) String tripId,
        @Size(max = 256) String legId,
        @NotNull @Pattern(regexp = "transport|stay") String kind,
        @Size(max = 200) String title,
        @Size(max = 32) String mode,
        @Size(max = 200) String fromLabel,
        @Size(max = 200) String toLabel,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}") String departAt,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}") String arriveAt,
        @Size(max = 200) String propertyName,
        @Size(max = 500) String address,
        @Size(max = 120) String city,
        @Size(max = 120) String country,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        @Min(0) Long budgetAmount,
        @Size(max = 32) String budgetStatus,
        LocalDate budgetDate,
        @Size(max = 200) String provider,
        @Size(max = 200) String confirmationCode,
        @Size(max = 2048) String link,
        @Size(max = 4000) String notes) {
}
