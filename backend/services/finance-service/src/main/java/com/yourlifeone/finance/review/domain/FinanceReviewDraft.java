package com.yourlifeone.finance.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourlifeone.finance.shared.validation.IsoDate;
import com.yourlifeone.finance.shared.validation.Limits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * The dated banking review as the app or an AI agent writes it: cash flow by month, budget assumptions,
 * spending opportunities, loans and the sources behind them. Amounts are integer pence in GBP; nothing here
 * is a live bank balance. Field rules are annotations; rules across fields are in {@code ReviewRules}.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinanceReviewDraft(
        @NotNull @IsoDate String asOf,
        @NotNull @Pattern(regexp = FinanceReviewDraft.CURRENCY) String currency,
        @NotNull @Valid ReviewCoverage coverage,
        @NotNull @Size(max = FinanceReviewDraft.MAX_ACCOUNTS) List<@NotNull @Valid ReviewAccount> accounts,
        @NotNull @Size(max = FinanceReviewDraft.MAX_MONTHS) List<@NotNull @Valid ReviewMonth> months,
        @NotNull @Valid ReviewBudget budget,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Size(max = FinanceReviewDraft.MAX_EXPENSE_GROUPS)
        List<@NotNull @Valid ReviewExpenseGroup> expenseGroups,
        @NotNull @Size(max = FinanceReviewDraft.MAX_OPPORTUNITIES) List<@NotNull @Valid ReviewOpportunity> opportunities,
        @NotNull @Size(max = FinanceReviewDraft.MAX_LOANS) List<@NotNull @Valid ReviewLoan> loans,
        @NotNull @Size(max = FinanceReviewDraft.MAX_NOTES) List<@NotNull @Size(max = Limits.MAX_TEXT) String> notes,
        @NotNull @Size(max = FinanceReviewDraft.MAX_SOURCES) List<@NotNull @Valid ReviewSource> sources,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Size(max = FinanceReviewDraft.MAX_LOAN_PROFILES) List<@NotNull @Valid LoanOverpaymentProfile> loanProfiles,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Valid ReviewBudgetForecast budgetForecast,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Size(max = FinanceReviewDraft.MAX_TRANSACTIONS)
        List<@NotNull @Valid ReviewSpendingTransaction> transactions) {
    public static final String CURRENCY = "GBP";
    public static final int MAX_ACCOUNTS = 50;
    public static final int MAX_MONTHS = 120;
    public static final int MAX_OPPORTUNITIES = 100;
    public static final int MAX_EXPENSE_GROUPS = 500;
    public static final int MAX_LOANS = 50;
    public static final int MAX_LOAN_PROFILES = 20;
    public static final int MAX_TRANSACTIONS = 5_000;
    public static final int MAX_NOTES = 100;
    public static final int MAX_SOURCES = 100;
}
