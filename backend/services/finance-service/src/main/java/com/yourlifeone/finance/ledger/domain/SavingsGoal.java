package com.yourlifeone.finance.ledger.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/**
 * Money being saved towards a target, in integer pence.
 *
 * @param linkedAccountId the account it is saved in, or null
 * @param completedAt when it was marked completed; null while it is not
 */
@Builder(toBuilder = true)
public record SavingsGoal(String id, String name, long targetAmount, long currentAmount, String linkedAccountId,
        String icon, LocalDate deadline, boolean completed, Instant completedAt, Instant createdAt, Instant updatedAt) {
}
