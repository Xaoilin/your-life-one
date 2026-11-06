package com.yourlifeone.finance.ledger.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.ledger.config.LedgerProperties;
import com.yourlifeone.finance.ledger.domain.Account;
import com.yourlifeone.finance.ledger.domain.Budget;
import com.yourlifeone.finance.ledger.domain.SavingsGoal;
import com.yourlifeone.finance.ledger.domain.Transaction;
import com.yourlifeone.finance.shared.AllowedValues;
import com.yourlifeone.finance.shared.RecordIds;
import com.yourlifeone.finance.shared.Texts;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Checks and tidies the ledger records the app saves, against the configured options and limits. */
@Component
@RequiredArgsConstructor
public class LedgerRules {
    private final LedgerProperties properties;

    public Account account(Account requested) {
        RecordIds.require(requested.id());
        if (Math.abs(requested.balance()) > properties.maxBalance()) {
            throw ApiException.badRequest("invalid_balance", "balance must be within " + properties.maxBalance() + " pence.");
        }
        return requested.toBuilder()
                .name(name(requested.name()))
                .type(AllowedValues.require("type", requested.type(), properties.accountTypes()))
                .color(Texts.trimmed(requested.color()))
                .icon(Texts.trimmed(requested.icon()))
                .build();
    }

    public Transaction transaction(Transaction requested) {
        RecordIds.require(requested.id());
        RecordIds.require(requested.accountId());
        var type = AllowedValues.require("type", requested.type(), properties.transactionTypes());
        requireAmount("amount", requested.amount(), 1);
        return requested.toBuilder()
                .type(type)
                .category(AllowedValues.require("category", requested.category(), properties.transactionCategories()))
                .toAccountId(receivingAccount(type, requested))
                .description(description(requested.description()))
                .tags(tags(requested.tags()))
                .build();
    }

    public Budget budget(Budget requested) {
        RecordIds.require(requested.id());
        requireAmount("monthlyLimit", requested.monthlyLimit(), 0);
        return requested.toBuilder()
                .category(AllowedValues.require("category", requested.category(), properties.expenseCategories()))
                .build();
    }

    public SavingsGoal savingsGoal(SavingsGoal requested) {
        RecordIds.require(requested.id());
        requireAmount("targetAmount", requested.targetAmount(), 0);
        requireAmount("currentAmount", requested.currentAmount(), 0);
        var linked = Texts.blankToNull(requested.linkedAccountId());
        if (linked != null) RecordIds.require(linked);
        return requested.toBuilder()
                .name(name(requested.name()))
                .linkedAccountId(linked)
                .icon(Texts.trimmed(requested.icon()))
                .build();
    }

    /** A transfer names another account to receive the money; income and expense name none. */
    private static String receivingAccount(String type, Transaction requested) {
        var to = Texts.blankToNull(requested.toAccountId());
        if (!Transaction.TRANSFER.equals(type)) {
            if (to != null) {
                throw ApiException.badRequest("invalid_to_account", "Only a transfer has a toAccountId.");
            }
            return null;
        }
        if (to == null || to.equals(requested.accountId())) {
            throw ApiException.badRequest("invalid_to_account", "A transfer needs a toAccountId other than its accountId.");
        }
        return RecordIds.require(to);
    }

    private void requireAmount(String field, long amount, long minimum) {
        if (amount < minimum || amount > properties.maxAmount()) {
            throw ApiException.badRequest("invalid_amount",
                    field + " must be between " + minimum + " and " + properties.maxAmount() + " pence.");
        }
    }

    private String name(String requested) {
        var name = Texts.trimmed(requested);
        if (name.isEmpty() || name.length() > properties.maxNameLength()) {
            throw ApiException.badRequest("invalid_name", "name must be 1-" + properties.maxNameLength() + " characters.");
        }
        return name;
    }

    private String description(String requested) {
        var description = requested == null ? "" : requested;
        if (description.length() > properties.maxDescriptionLength()) {
            throw ApiException.badRequest("invalid_description",
                    "description must be at most " + properties.maxDescriptionLength() + " characters.");
        }
        return description;
    }

    private List<String> tags(List<String> requested) {
        var tags = Texts.distinctTrimmed(requested);
        if (tags.size() > properties.maxTags() || tags.stream().anyMatch(tag -> tag.length() > properties.maxTagLength())) {
            throw ApiException.badRequest("invalid_tags", "A transaction has at most " + properties.maxTags()
                    + " tags of at most " + properties.maxTagLength() + " characters.");
        }
        return tags;
    }
}
