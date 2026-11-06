package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * Dated what-if share prices, never cash.
 *
 * @param withholdingRate the share of proceeds withheld, from 0 to 1
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquityScenario(
        @NotNull @Size(max = EquityScenario.MAX_PRICES)
        List<@NotNull @DecimalMin("0") @DecimalMax(Limits.MAX_MONEY) Double> pricesUsd,
        @NotNull @DecimalMin("0") @DecimalMax("1") Double withholdingRate,
        @NotNull @Positive @DecimalMax(Limits.MAX_USD_TO_GBP) Double usdToGbp,
        @NotNull @IsoDate String asOf,
        @NotNull @Size(max = Limits.MAX_LONG_TEXT) String notes) {
    public static final int MAX_PRICES = 20;
}
