package com.yourlifeone.profile.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * A user's connection record for one provider (the provider is in the path).
 *
 * @param lastError the connection error to show; null or blank when there is none
 */
public record IntegrationRequest(
        @NotNull @Size(max = 32) String status,
        Instant configuredAt,
        @Size(max = 500) String lastError) {
}
