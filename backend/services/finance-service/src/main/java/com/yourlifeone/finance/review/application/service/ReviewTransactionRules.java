package com.yourlifeone.finance.review.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.review.domain.FinanceReviewDraft;
import com.yourlifeone.finance.review.domain.ReviewAccount;
import com.yourlifeone.finance.review.domain.ReviewSource;
import com.yourlifeone.finance.review.domain.ReviewSpendingTransaction;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Keeps itemised evidence tied to personal accounts and known review sources, without inferring payments. */
@Component
public class ReviewTransactionRules {
    private static final String PERSONAL = "personal";
    private static final String BANK = "bank";
    private static final String INVALID_REVIEW = "invalid_review";
    private static final int MONTH_LENGTH = 7;

    public void check(FinanceReviewDraft draft) {
        if (draft.transactions() == null) return;
        var personalAccounts = draft.accounts().stream().filter(account -> PERSONAL.equals(account.ownership()))
                .map(ReviewAccount::id).collect(Collectors.toSet());
        var sources = draft.sources().stream().map(ReviewSource::id).collect(Collectors.toSet());
        var ids = new HashSet<String>();
        for (var transaction : draft.transactions()) {
            if (!ids.add(transaction.id())) throw invalid("Duplicate transaction IDs.");
            requirePersonalAccount(transaction, personalAccounts);
            requireDateAndEvidence(transaction, sources);
            if (transaction.amountPence() == 0) throw invalid("Transaction amounts must be nonzero.");
        }
    }

    private static void requirePersonalAccount(ReviewSpendingTransaction transaction, Set<String> personalAccounts) {
        if (!personalAccounts.contains(transaction.accountId())) {
            throw invalid("Spending transactions must reference a personal review account.");
        }
    }

    private static void requireDateAndEvidence(ReviewSpendingTransaction transaction, Set<String> sources) {
        if (transaction.date() != null && !transaction.date().substring(0, MONTH_LENGTH).equals(transaction.month())) {
            throw invalid("Transaction date must match its month.");
        }
        if (BANK.equals(transaction.origin()) && (transaction.date() == null
                || transaction.sourceIds() == null || transaction.sourceIds().isEmpty())) {
            throw invalid("Bank transactions require a payment date and source evidence.");
        }
        if (transaction.sourceIds() != null && !sources.containsAll(transaction.sourceIds())) {
            throw invalid("Transaction references an unknown source.");
        }
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest(INVALID_REVIEW, message);
    }
}
