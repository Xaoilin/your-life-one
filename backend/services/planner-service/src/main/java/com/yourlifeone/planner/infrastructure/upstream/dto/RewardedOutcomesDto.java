package com.yourlifeone.planner.infrastructure.upstream.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** The prayer service's {@code GET /api/prayer/v1/outcomes/rewarded}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RewardedOutcomesDto(List<RewardedOutcomeDto> outcomes) {
}
