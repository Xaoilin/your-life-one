package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * A suggested product review drawn from the insights; nothing changes automatically.
 *
 * @param confidence {@code low}, {@code medium} or {@code high}, from the sample size
 */
@Builder
public record Recommendation(
        String id,
        String title,
        String summary,
        String evidence,
        String confidence,
        String suggestedAction) {
}
