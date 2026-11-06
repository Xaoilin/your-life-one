package com.yourlifeone.finance.ledger.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Money in, out, or between two of the user's accounts.
 *
 * @param amount integer pence, always positive; the type says which way it moves
 * @param toAccountId the receiving account of a transfer; null for income and expense
 */
@Builder(toBuilder = true)
public record Transaction(String id, String type, long amount, String category, String accountId, String toAccountId,
        String description, LocalDate date, List<String> tags, Instant createdAt, Instant updatedAt) {
    public static final String INCOME = "income";
    public static final String EXPENSE = "expense";
    public static final String TRANSFER = "transfer";

    public boolean transfer() {
        return TRANSFER.equals(type);
    }
}
