package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/** A named scenario, never a lender instruction or a current balance. */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoanOverpaymentProfile(
        @NotBlank @Size(max = Limits.MAX_ID) String id,
        @NotBlank @Size(max = 128) String name,
        @NotNull Boolean isDefault,
        @NotNull @Pattern(regexp = Limits.MONTH) String firstPaymentMonth,
        @Size(min = 1, max = Limits.MAX_ID) String selectedLoanId,
        @NotNull Boolean showShare,
        @NotNull @IsoDate String reviewAsOf,
        @NotNull @Size(min = 1, max = FinanceReviewDraft.MAX_LOANS) List<@NotNull @Valid LoanOverpaymentProfileLoan> loans) {
}
