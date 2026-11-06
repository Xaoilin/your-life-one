package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * How often one feature/action path occurred in the current window and in the equal window before it.
 */
@Builder
public record PathCount(String feature, String action, long currentCount, long previousCount) {
}
