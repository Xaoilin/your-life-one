package com.yourlifeone.planner.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** A clock the tests move by hand, so days and time zones can be crossed on purpose. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClock {
    @Bean
    @Primary
    MovableClock movableClock() {
        return new MovableClock(Instant.parse("2026-09-27T12:00:00Z"));
    }

    /** The service's clock, set by each test. */
    public static final class MovableClock extends Clock {
        private volatile Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        public void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
