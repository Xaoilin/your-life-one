package com.yourlifeone.commonservice.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * One Bucket4j token bucket per user, held in memory. Each service instance limits its own traffic;
 * there is one instance per service today, so this is the whole limit.
 */
public class UserRateLimiter {
    private final RateLimitProperties properties;
    private final TimeMeter clock;
    private final Cache<String, Bucket> buckets;

    public UserRateLimiter(RateLimitProperties properties) {
        this(properties, TimeMeter.SYSTEM_MILLISECONDS);
    }

    /** @param clock the buckets' time source; tests pass a controllable one */
    UserRateLimiter(RateLimitProperties properties, TimeMeter clock) {
        this.properties = properties;
        this.clock = clock;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.maxTrackedUsers())
                .expireAfterAccess(properties.idleExpiry())
                .build();
    }

    /** Takes one request from the user's bucket. Thread-safe: concurrent requests never overspend it. */
    public RateLimitDecision tryAcquire(String userId) {
        var probe = buckets.get(userId, ignored -> newBucket()).tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.allow();
        }
        return RateLimitDecision.refuse(wholeSecondsRoundedUp(probe.getNanosToWaitForRefill()));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(properties.burst())
                        .refillGreedy(properties.requestsPerMinute(), Duration.ofMinutes(1)))
                .withCustomTimePrecision(clock)
                .build();
    }

    private static long wholeSecondsRoundedUp(long nanos) {
        return (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1);
    }
}
