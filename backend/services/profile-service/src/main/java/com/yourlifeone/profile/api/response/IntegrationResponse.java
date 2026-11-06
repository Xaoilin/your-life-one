package com.yourlifeone.profile.api.response;

import java.time.Instant;

public record IntegrationResponse(
        String provider, String status, Instant configuredAt, String lastError, Instant updatedAt) {
}
