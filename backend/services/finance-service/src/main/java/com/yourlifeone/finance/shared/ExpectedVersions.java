package com.yourlifeone.finance.shared;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Optimistic concurrency by {@code updatedAt}: an edit names the version it changed, and is refused when
 * the stored record has moved on since.
 */
public final class ExpectedVersions {
    private ExpectedVersions() {
    }

    /** True when {@code expected} is the same instant as {@code stored}, however its offset is written. */
    public static boolean matches(String expected, Instant stored) {
        if (expected == null || stored == null) return false;
        try {
            return OffsetDateTime.parse(expected).toInstant().equals(stored);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
