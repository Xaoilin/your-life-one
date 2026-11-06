package com.yourlifeone.profile.activity.api.response;

import lombok.Builder;

@Builder
public record FunnelStageResponse(String id, String label, long sessionCount, Integer conversionPercent) {
}
