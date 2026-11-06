package com.yourlifeone.finance.review.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.review.config.ReviewProperties;
import com.yourlifeone.finance.review.domain.FinanceReviewDraft;
import com.yourlifeone.finance.review.domain.LoanOverpaymentProfile;
import com.yourlifeone.finance.review.domain.LoanOverpaymentProfileLoan;
import com.yourlifeone.finance.review.domain.ReviewAccount;
import com.yourlifeone.finance.review.domain.ReviewBudgetForecastItem;
import com.yourlifeone.finance.review.domain.ReviewCoverage;
import com.yourlifeone.finance.review.domain.ReviewLoan;
import com.yourlifeone.finance.review.domain.ReviewMonth;
import com.yourlifeone.finance.review.domain.ReviewSource;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The review's rules across fields (each field's own rules are annotations on the draft): monthly net is
 * income minus outflow, IDs and months are unique, a balance or settlement comes with its date, a loan's
 * dates are in order and its sources exist, the coverage dates agree, and the whole review is bounded.
 * ISO dates and months compare correctly as text.
 */
@Component
@RequiredArgsConstructor
public class ReviewRules {
    private static final int MONTH_LENGTH = 7;

    private final ReviewProperties properties;
    private final ReviewTransactionRules transactions;
    private final JsonMapper json;

    public FinanceReviewDraft check(FinanceReviewDraft draft) {
        requireCoverage(draft.coverage());
        transactions.check(draft);
        draft.months().forEach(ReviewRules::requireNet);
        requireUnique(draft.accounts(), ReviewAccount::id, "Duplicate account IDs.");
        requireUnique(draft.months(), ReviewMonth::month, "Duplicate months.");
        requireUnique(draft.loans(), ReviewLoan::id, "Duplicate loan IDs.");
        requireUnique(draft.sources(), ReviewSource::id, "Duplicate source IDs.");
        if (draft.expenseGroups() != null) {
            requireUnique(draft.expenseGroups(), group -> group.label().strip().toLowerCase(Locale.ROOT),
                    "Duplicate expense group labels.");
        }
        if (draft.budgetForecast() != null) {
            requireUnique(draft.budgetForecast().items(), ReviewBudgetForecastItem::id, "Duplicate forecast item IDs.");
        }
        draft.loans().forEach(ReviewRules::requireLoanDates);
        requireKnownSources(draft);
        requireProfiles(draft);
        checkSize(draft);
        return draft;
    }

    private static void requireCoverage(ReviewCoverage coverage) {
        if (coverage.from().compareTo(coverage.to()) > 0
                || coverage.completeThrough().compareTo(coverage.to().substring(0, MONTH_LENGTH)) > 0) {
            throw invalid("Coverage dates are inconsistent.");
        }
    }

    private static void requireNet(ReviewMonth month) {
        if (month.incomePence() - month.outflowPence() != month.netPence()) {
            throw invalid("Monthly net must equal income minus outflow.");
        }
    }

    private static void requireLoanDates(ReviewLoan loan) {
        if ((loan.balancePence() == null) != (loan.balanceAsOf() == null)
                || (loan.settlementPence() == null) != (loan.settlementAsOf() == null)) {
            throw invalid("Balances and settlements require their own dates.");
        }
        if (loan.startDate() != null && loan.endDate() != null && loan.startDate().compareTo(loan.endDate()) > 0) {
            throw invalid("Loan end precedes its start.");
        }
    }

    private static void requireKnownSources(FinanceReviewDraft draft) {
        var sources = draft.sources().stream().map(ReviewSource::id).toList();
        if (!draft.loans().stream().allMatch(loan -> sources.containsAll(loan.sourceIds()))) {
            throw invalid("Loan references an unknown source.");
        }
    }

    private void requireProfiles(FinanceReviewDraft draft) {
        if (draft.loanProfiles() == null) return;
        requireUnique(draft.loanProfiles(), LoanOverpaymentProfile::id, "Duplicate loan profile IDs.");
        if (draft.loanProfiles().stream().filter(profile -> Boolean.TRUE.equals(profile.isDefault())).count() > 1) {
            throw invalid("Only one loan profile can be the default.");
        }
        draft.loanProfiles().forEach(profile -> checkProfile(profile, draft.loans()));
    }

    public void checkProfile(LoanOverpaymentProfile profile, List<ReviewLoan> loans) {
        requireUnique(profile.loans(), LoanOverpaymentProfileLoan::loanId, "Duplicate profile loan IDs.");
        var known = loans.stream().map(ReviewLoan::id).toList();
        var ids = profile.loans().stream().map(LoanOverpaymentProfileLoan::loanId).toList();
        if (!known.containsAll(ids)) throw invalid("Loan profile references an unknown loan.");
        if (profile.selectedLoanId() != null && !ids.contains(profile.selectedLoanId())) {
            throw invalid("The selected loan must be in the profile.");
        }
        if (profile.loans().stream().anyMatch(loan -> loan.balancePence() > 0 && loan.monthlyPaymentPence() == 0)) {
            throw invalid("A positive loan balance needs a positive monthly payment.");
        }
    }

    void checkSize(FinanceReviewDraft draft) {
        if (json.writeValueAsBytes(draft).length > properties.maxDocumentBytes()) {
            throw invalid("The review must fit within " + properties.maxDocumentBytes() + " bytes.");
        }
    }

    private static <T> void requireUnique(List<T> entries, Function<T, String> key, String message) {
        var seen = new HashSet<String>();
        if (!entries.stream().map(key).allMatch(seen::add)) throw invalid(message);
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("invalid_review", message);
    }
}
