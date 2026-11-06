package com.yourlifeone.finance.ledger.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record SavingsGoalResponse(String id, String name, long targetAmount, long currentAmount,
        String linkedAccountId, String icon, LocalDate deadline, boolean completed, Instant completedAt,
        Instant createdAt, Instant updatedAt) {
}
