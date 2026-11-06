package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** One leg; the ID is in the path. A new leg joins the end of the route. */
public record LegRequest(
        @NotBlank @Size(max = 256) String tripId,
        @NotBlank @Size(max = 120) String country,
        @NotBlank @Size(max = 120) String city,
        LocalDate startDate,
        LocalDate endDate) {
}
