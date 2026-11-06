package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * Which events the insights cover. A null filter matches every event.
 *
 * @param rangeDays the current window, ending now; trends compare with the equal window before it
 */
@Builder
public record ActivityFilters(int rangeDays, String kind, String surface, String outcome, String feature) {
}
