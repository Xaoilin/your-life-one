package com.yourlifeone.life.trips.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** @param direction -1 moves the leg one place earlier, 1 one place later */
public record MoveLegRequest(@NotNull @Min(-1) @Max(1) Integer direction) {
}
