package com.yourlifeone.prayer.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A user's prayer preferences and tracking state. Location is a global setting owned by the
 * profile service and is passed in by the caller.
 *
 * @param importedAt when legacy tracking was imported; null if never
 */
@Builder(toBuilder = true)
public record PrayerProfile(
        boolean enabled, boolean reminderEnabled, int reminderMinutes, Tracking tracking, Instant importedAt) {
}
