package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * A loan, with what is known about it and how. Amounts are pence; optional values are null when unknown.
 * A balance and a settlement quote each come with the date they were true on.
 *
 * @param status {@code active}, {@code repaid} or {@code closed}
 * @param balanceKind how the balance is known: {@code statement}, {@code settlement}, {@code estimate} or {@code unknown}
 * @param sourceIds IDs of the review's sources this loan's figures come from
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewLoan(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = Limits.MAX_TEXT) String lender,
        @NotNull @Size(max = Limits.MAX_TEXT) String purpose,
        @NotNull @Pattern(regexp = "active|repaid|closed") String status,
        @Min(0) @Max(Limits.MAX_PENCE) Long monthlyPaymentPence,
        @Min(0) @Max(Limits.MAX_PENCE) Long userSharePence,
        @Min(0) @Max(Limits.MAX_PENCE) Long balancePence,
        @IsoDate String balanceAsOf,
        @NotNull @Pattern(regexp = "statement|settlement|estimate|unknown") String balanceKind,
        @IsoDate String startDate,
        @IsoDate String endDate,
        @DecimalMin("0") @DecimalMax(Limits.MAX_APR_PERCENT) Double aprPercent,
        @Min(0) @Max(Limits.MAX_PENCE) Long settlementPence,
        @IsoDate String settlementAsOf,
        @IsoDate String nextPaymentDate,
        @Min(0) @Max(Limits.MAX_COUNT) Long paymentsRemaining,
        @Min(0) @Max(Limits.MAX_PENCE) Long originalPrincipalPence,
        @NotNull @Size(max = Limits.MAX_TEXT) String rateNote,
        @NotNull @Size(max = Limits.MAX_TEXT) String notes,
        @NotNull @Size(max = ReviewLoan.MAX_SOURCES) List<@NotBlank @Size(max = Limits.MAX_ID) String> sourceIds) {
    public static final int MAX_SOURCES = 100;
}
