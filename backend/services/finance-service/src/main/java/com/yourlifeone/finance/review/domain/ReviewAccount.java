package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * A bank account's dated balance in the review (not a live balance).
 *
 * @param ownership {@code personal} or {@code household}
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewAccount(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = Limits.MAX_TEXT) String label,
        @NotNull @Pattern(regexp = "personal|household") String ownership,
        @NotNull @Min(Limits.MIN_SIGNED_PENCE) @Max(Limits.MAX_PENCE) Long balancePence,
        @NotNull @IsoDate String asOf) {
}
