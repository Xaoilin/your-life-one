package com.yourlifeone.profile.api.response;

import java.time.Instant;

public record ViewPreferenceResponse(String key, String value, Instant updatedAt) {
}
