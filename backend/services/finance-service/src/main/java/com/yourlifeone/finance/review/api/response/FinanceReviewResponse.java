package com.yourlifeone.finance.review.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourlifeone.finance.review.domain.LoanOverpaymentProfile;
import com.yourlifeone.finance.review.domain.ReviewAccount;
import com.yourlifeone.finance.review.domain.ReviewBudget;
import com.yourlifeone.finance.review.domain.ReviewBudgetForecast;
import com.yourlifeone.finance.review.domain.ReviewCoverage;
import com.yourlifeone.finance.review.domain.ReviewExpenseGroup;
import com.yourlifeone.finance.review.domain.ReviewLoan;
import com.yourlifeone.finance.review.domain.ReviewMonth;
import com.yourlifeone.finance.review.domain.ReviewOpportunity;
import com.yourlifeone.finance.review.domain.ReviewSource;
import com.yourlifeone.finance.review.domain.ReviewSpendingTransaction;
import java.time.Instant;
import java.util.List;

/** The saved review; absent classifications are omitted, and unknown financial values remain null. */
public record FinanceReviewResponse(String id, String asOf, String currency, ReviewCoverage coverage,
        List<ReviewAccount> accounts, List<ReviewMonth> months, ReviewBudget budget,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ReviewExpenseGroup> expenseGroups,
        List<ReviewOpportunity> opportunities, List<ReviewLoan> loans, List<String> notes, List<ReviewSource> sources,
        Instant createdAt, Instant updatedAt,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<LoanOverpaymentProfile> loanProfiles,
        @JsonInclude(JsonInclude.Include.NON_NULL) ReviewBudgetForecast budgetForecast,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ReviewSpendingTransaction> transactions) {
}
