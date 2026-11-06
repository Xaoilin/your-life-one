package com.yourlifeone.profile.activity.api.response;

import lombok.Builder;

@Builder
public record RecommendationResponse(
        String id,
        String title,
        String summary,
        String evidence,
        String confidence,
        String suggestedAction) {
}
