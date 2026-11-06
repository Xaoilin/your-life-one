package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * One step of the session funnel.
 *
 * @param id {@code sessions}, {@code surface_views} or {@code successful_opens}
 * @param conversionPercent whole-number share of started sessions that reached this step
 */
@Builder
public record FunnelStage(String id, String label, long sessionCount, Integer conversionPercent) {
}
