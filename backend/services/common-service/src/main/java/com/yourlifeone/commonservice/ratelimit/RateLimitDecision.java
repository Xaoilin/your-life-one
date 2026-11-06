package com.yourlifeone.commonservice.ratelimit;

/**
 * Whether one request may proceed.
 *
 * @param retryAfterSeconds when refused, whole seconds until the user's next request is allowed; 0 when allowed
 */
public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    public static RateLimitDecision allow() {
        return new RateLimitDecision(true, 0);
    }

    public static RateLimitDecision refuse(long retryAfterSeconds) {
        return new RateLimitDecision(false, Math.max(1, retryAfterSeconds));
    }
}
