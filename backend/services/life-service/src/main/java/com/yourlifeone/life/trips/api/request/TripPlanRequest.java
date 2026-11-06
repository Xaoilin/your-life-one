package com.yourlifeone.life.trips.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Creates a trip, its route and its first bookings together, or none of them. */
public record TripPlanRequest(
        @NotBlank @Size(max = 256) String id,
        @NotNull @Valid TripRequest trip,
        @NotNull @Size(max = 100) List<@Valid PlanLegRequest> legs,
        @NotNull @Size(max = 200) List<@Valid BookingRequest> bookings) {
}
