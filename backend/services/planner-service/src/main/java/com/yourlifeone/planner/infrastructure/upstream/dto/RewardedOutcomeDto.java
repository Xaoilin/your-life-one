package com.yourlifeone.planner.infrastructure.upstream.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;

/** One item of the prayer service's {@code GET /api/prayer/v1/outcomes/rewarded}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RewardedOutcomeDto(LocalDate date, String prayer, Instant recordedAt, String taskId) {
}
