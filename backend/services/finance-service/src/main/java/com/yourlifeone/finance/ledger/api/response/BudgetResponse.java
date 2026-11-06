package com.yourlifeone.finance.ledger.api.response;

import java.time.Instant;

public record BudgetResponse(String id, String category, long monthlyLimit, Instant createdAt, Instant updatedAt) {
}
