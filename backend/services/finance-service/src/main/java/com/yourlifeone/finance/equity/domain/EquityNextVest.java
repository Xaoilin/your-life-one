package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** @param alternateDate another date the vest may happen on, or null */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquityNextVest(
        @NotNull @IsoDate String date,
        @IsoDate String alternateDate,
        @NotNull @Min(0) @Max(Limits.MAX_QUANTITY) Long quantity,
        @NotNull @Size(max = Limits.MAX_TEXT) String condition) {
}
