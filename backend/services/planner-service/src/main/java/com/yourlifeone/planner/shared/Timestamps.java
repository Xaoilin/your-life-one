package com.yourlifeone.planner.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Stored times are kept to the millisecond, the precision a browser's {@code Date} keeps, so a time
 * the app echoes back (for example as an expected version) always equals the stored one.
 */
public final class Timestamps {
    private Timestamps() {
    }

    public static Instant now(Clock clock) {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
