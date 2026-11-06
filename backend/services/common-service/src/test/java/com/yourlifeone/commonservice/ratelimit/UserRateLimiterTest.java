package com.yourlifeone.commonservice.ratelimit;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRateLimiterTest {
    private final ManualClock clock = new ManualClock();

    /** 60 requests a minute (one a second), at most 3 at once. */
    private UserRateLimiter limiter() {
        return new UserRateLimiter(new RateLimitProperties(true, 60, 3, 100, Duration.ofMinutes(10)), clock);
    }

    @Test
    void allowsTheBurstThenRefusesWithTheWaitUntilTheNextToken() {
        var limiter = limiter();

        for (int request = 0; request < 3; request++) {
            assertTrue(limiter.tryAcquire("user").allowed());
        }
        var refused = limiter.tryAcquire("user");

        assertFalse(refused.allowed());
        assertEquals(1, refused.retryAfterSeconds());
    }

    @Test
    void refillsAtTheConfiguredRate() {
        var limiter = limiter();
        for (int request = 0; request < 3; request++) limiter.tryAcquire("user");

        clock.advance(Duration.ofSeconds(1));

        assertTrue(limiter.tryAcquire("user").allowed());
        assertFalse(limiter.tryAcquire("user").allowed());
    }

    @Test
    void eachUserHasTheirOwnBucket() {
        var limiter = limiter();
        for (int request = 0; request < 3; request++) limiter.tryAcquire("busy");

        assertFalse(limiter.tryAcquire("busy").allowed());
        assertTrue(limiter.tryAcquire("quiet").allowed());
    }

    @Test
    void concurrentRequestsNeverSpendMoreThanTheBurst() throws Exception {
        var limiter = new UserRateLimiter(new RateLimitProperties(true, 1, 25, 100, Duration.ofMinutes(10)), clock);
        var allowed = new AtomicInteger();
        var start = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(16)) {
            for (int request = 0; request < 200; request++) {
                threads.submit(() -> {
                    start.await();
                    if (limiter.tryAcquire("user").allowed()) allowed.incrementAndGet();
                    return null;
                });
            }
            start.countDown();
        }

        assertEquals(25, allowed.get());
    }

    @Test
    void retryAfterRoundsUpToWholeSeconds() {
        // One request a minute: the next token is 60 seconds away.
        var limiter = new UserRateLimiter(new RateLimitProperties(true, 1, 1, 100, Duration.ofMinutes(10)), clock);
        limiter.tryAcquire("user");
        clock.advance(Duration.ofMillis(500));

        assertEquals(60, limiter.tryAcquire("user").retryAfterSeconds());
    }

    private static final class ManualClock implements TimeMeter {
        private volatile long nanos;

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }
}
