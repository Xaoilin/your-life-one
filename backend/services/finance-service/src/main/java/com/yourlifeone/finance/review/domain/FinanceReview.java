package com.yourlifeone.finance.review.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * The user's saved banking review: the draft plus its identity ({@code current}, one per user) and times.
 * {@code updatedAt} is its version: a save names the one it replaces.
 */
@Builder
public record FinanceReview(String id, String asOf, String currency, ReviewCoverage coverage,
        List<ReviewAccount> accounts, List<ReviewMonth> months, ReviewBudget budget, List<ReviewExpenseGroup> expenseGroups,
        List<ReviewOpportunity> opportunities, List<ReviewLoan> loans, List<String> notes, List<ReviewSource> sources,
        Instant createdAt, Instant updatedAt, List<LoanOverpaymentProfile> loanProfiles, ReviewBudgetForecast budgetForecast,
        List<ReviewSpendingTransaction> transactions) {
    public static final String CURRENT = "current";
}
