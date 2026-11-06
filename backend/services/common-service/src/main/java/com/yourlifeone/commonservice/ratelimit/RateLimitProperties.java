package com.yourlifeone.commonservice.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-user request limits ({@code app.rate-limit.*}). Each signed-in user gets a token bucket:
 * {@code burst} requests at once, refilled at {@code requestsPerMinute}.
 *
 * @param enabled false turns limiting off
 * @param requestsPerMinute sustained requests a user may make each minute
 * @param burst requests a user may make at once before the rate applies
 * @param maxTrackedUsers users whose buckets are kept in memory; the least recently seen are dropped first
 * @param idleExpiry a user's bucket is forgotten (so starts full) after this long without a request
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        Boolean enabled, Integer requestsPerMinute, Integer burst, Integer maxTrackedUsers, Duration idleExpiry) {
    public RateLimitProperties {
        enabled = enabled == null || enabled;
        requestsPerMinute = requirePositive(requestsPerMinute == null ? 120 : requestsPerMinute, "requests-per-minute");
        burst = requirePositive(burst == null ? 60 : burst, "burst");
        maxTrackedUsers = requirePositive(maxTrackedUsers == null ? 10_000 : maxTrackedUsers, "max-tracked-users");
        idleExpiry = idleExpiry == null ? Duration.ofMinutes(10) : idleExpiry;
        if (idleExpiry.isNegative() || idleExpiry.isZero()) {
            throw new IllegalArgumentException("app.rate-limit.idle-expiry must be positive");
        }
    }

    private static int requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException("app.rate-limit." + name + " must be at least 1");
        }
        return value;
    }
}
