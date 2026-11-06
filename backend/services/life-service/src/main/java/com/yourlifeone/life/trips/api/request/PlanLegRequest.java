package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** A leg of a new plan, in route order; bookings refer to it by this ID. */
public record PlanLegRequest(
        @NotBlank @Size(max = 256) String id,
        @NotBlank @Size(max = 120) String country,
        @NotBlank @Size(max = 120) String city,
        LocalDate startDate,
        LocalDate endDate) {
}
