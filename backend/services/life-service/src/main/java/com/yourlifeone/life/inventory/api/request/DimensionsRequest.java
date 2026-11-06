package com.yourlifeone.life.inventory.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DimensionsRequest(
        @Positive BigDecimal length,
        @Positive BigDecimal width,
        @Positive BigDecimal height,
        @NotNull @Size(max = 8) String unit) {
}
