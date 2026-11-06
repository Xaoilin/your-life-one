package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.json.WholeNumberDeserializer;
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
import lombok.Builder;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Editable planning assumptions for one loan; the extra amount precedes optional matching. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanOverpaymentProfileLoan(
        @NotBlank @Size(max = Limits.MAX_ID) String loanId,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long balancePence,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double annualRatePercent,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long monthlyPaymentPence,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long monthlyExtraPence,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull @Min(0) @Max(Limits.MAX_PENCE) Long lumpSumPence,
        @JsonDeserialize(using = WholeNumberDeserializer.class) @NotNull @Min(0) @Max(1199) Long extraStartsAfterMonths,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double sharePercent,
        @NotNull Boolean matchExtra,
        @NotNull @Pattern(regexp = "shorten-term|reduce-payment") String repaymentMode,
        @IsoDate String balanceAsOf) {
}
