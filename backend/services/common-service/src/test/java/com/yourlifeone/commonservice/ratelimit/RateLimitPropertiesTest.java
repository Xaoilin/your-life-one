package com.yourlifeone.commonservice.ratelimit;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitPropertiesTest {
    @Test
    void defaultsToOnWithAModestPerUserLimit() {
        var properties = new RateLimitProperties(null, null, null, null, null);

        assertTrue(properties.enabled());
        assertEquals(120, properties.requestsPerMinute());
        assertEquals(60, properties.burst());
        assertEquals(10_000, properties.maxTrackedUsers());
        assertEquals(Duration.ofMinutes(10), properties.idleExpiry());
    }

    @Test
    void rejectsLimitsThatWouldRefuseEverything() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimitProperties(true, 0, 10, 10, null));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitProperties(true, 10, 0, 10, null));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitProperties(true, 10, 10, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitProperties(true, 10, 10, 10, Duration.ZERO));
    }
}
