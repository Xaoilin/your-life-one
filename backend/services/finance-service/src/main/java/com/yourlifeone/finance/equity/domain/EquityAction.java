package com.yourlifeone.finance.equity.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** @param dueDate when it is due, or null */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquityAction(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = Limits.MAX_TEXT) String title,
        @NotNull @Size(max = Limits.MAX_TEXT) String timing,
        @IsoDate String dueDate,
        @NotNull Boolean done) {
}
