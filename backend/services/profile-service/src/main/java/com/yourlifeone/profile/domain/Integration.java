package com.yourlifeone.profile.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A user's connection record for one integration provider.
 *
 * @param status connected, disconnected or error
 * @param configuredAt when the connection was last set up; null if never
 * @param lastError the last connection error shown to the user; null when there is none
 */
@Builder
public record Integration(String provider, String status, Instant configuredAt, String lastError, Instant updatedAt) {
}
