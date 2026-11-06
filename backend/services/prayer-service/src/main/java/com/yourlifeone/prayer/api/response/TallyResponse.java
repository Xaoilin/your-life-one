package com.yourlifeone.prayer.api.response;

import lombok.Builder;

@Builder
public record TallyResponse(
        int onTime,
        int late,
        int missed,
        int inferredMissed,
        int unclassified,
        int pending,
        int classifiedTotal,
        int opportunities,
        PercentagesResponse percentages) {
}
