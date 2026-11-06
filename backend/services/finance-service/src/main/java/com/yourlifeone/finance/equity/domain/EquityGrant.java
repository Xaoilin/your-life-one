package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * One option grant. Nothing here vests automatically.
 *
 * @param postEmploymentExpiry when the options lapse after leaving, or null when not known
 * @param nextVest the next vest, or null
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquityGrant(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotNull @IsoDate String grantDate,
        @NotNull @Min(0) @Max(Limits.MAX_QUANTITY) Long vested,
        @NotNull @Min(0) @Max(Limits.MAX_QUANTITY) Long unvested,
        @NotNull @DecimalMin("0") @DecimalMax(Limits.MAX_MONEY) Double strikeUsd,
        @NotNull @IsoDate String originalExpiry,
        @IsoDate String postEmploymentExpiry,
        @Valid EquityNextVest nextVest) {
}
