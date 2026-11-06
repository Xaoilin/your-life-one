package com.yourlifeone.finance.ledger.api.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** @param toAccountId null unless it is a transfer */
public record TransactionResponse(String id, String type, long amount, String category, String accountId,
        String toAccountId, String description, LocalDate date, List<String> tags, Instant createdAt,
        Instant updatedAt) {
}
