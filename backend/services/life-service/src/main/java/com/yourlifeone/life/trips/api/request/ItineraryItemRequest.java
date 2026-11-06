package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One day plan; the ID is in the path. Times are local HH:MM. */
public record ItineraryItemRequest(
        @NotBlank @Size(max = 256) String tripId,
        @NotBlank @Size(max = 256) String legId,
        LocalDate date,
        @NotBlank @Size(max = 200) String title,
        @Pattern(regexp = "\\d{2}:\\d{2}") String startTime,
        @Pattern(regexp = "\\d{2}:\\d{2}") String endTime,
        @Size(max = 200) String location,
        @Size(max = 4000) String notes,
        @Min(0) Integer sortOrder) {
}
