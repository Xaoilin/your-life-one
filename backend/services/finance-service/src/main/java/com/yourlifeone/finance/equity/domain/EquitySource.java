package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import com.yourlifeone.finance.shared.validation.WebUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** Where a figure came from, and when it was read. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquitySource(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @WebUrl String url,
        @NotNull @IsoDate String asOf) {
}
