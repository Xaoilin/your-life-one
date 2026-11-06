package com.yourlifeone.prayer.api.response;

import java.util.List;

/** Every rewarded outcome of the user, oldest recording first. */
public record RewardedOutcomesResponse(List<RewardedOutcomeResponse> outcomes) {
}
