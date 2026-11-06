package com.yourlifeone.finance.ledger.domain;

import java.time.Instant;
import lombok.Builder;

/** A monthly spending limit for one expense category, in integer pence. */
@Builder(toBuilder = true)
public record Budget(String id, String category, long monthlyLimit, Instant createdAt, Instant updatedAt) {
}
