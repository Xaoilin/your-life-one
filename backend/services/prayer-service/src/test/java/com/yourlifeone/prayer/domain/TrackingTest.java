package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static com.yourlifeone.prayer.domain.Fixtures.london;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingTest {
    private static final Instant NOON_UTC = Instant.parse("2026-09-25T12:00:00Z");

    @Test
    void activationDayCountsOnlyPrayersStillOpenWhenTrackingStarted() {
        var tracking = new Tracking(NOON_UTC, null, null).captureActivation(london(DAY));

        assertEquals(DAY, tracking.activationDate());
        assertEquals(List.of(PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA),
                tracking.activationPrayers());
        assertFalse(tracking.isTracked(london(DAY), PrayerName.FAJR, NOON_UTC));
        assertTrue(tracking.isTracked(london(DAY), PrayerName.DHUHR, NOON_UTC));
    }

    @Test
    void theFrozenActivationDayOutlivesLaterTimetableChanges() {
        var tracking = new Tracking(NOON_UTC, DAY, List.of(PrayerName.ISHA));

        assertFalse(tracking.isTracked(london(DAY), PrayerName.DHUHR, NOON_UTC.plusSeconds(86_400)));
        assertTrue(tracking.isTracked(london(DAY), PrayerName.ISHA, NOON_UTC.plusSeconds(86_400)));
        assertSame(tracking, tracking.captureActivation(london(DAY)));
    }

    @Test
    void daysBeforeTrackingAreNeverTrackedAndLaterDaysAlwaysAre() {
        var tracking = new Tracking(NOON_UTC, DAY, List.of());

        assertFalse(tracking.isTracked(london(DAY.minusDays(1)), PrayerName.ISHA, NOON_UTC));
        assertTrue(tracking.isTracked(london(DAY.plusDays(1)), PrayerName.FAJR, NOON_UTC));
    }

    @Test
    void doesNotCaptureActivationFromAnotherDay() {
        var tracking = new Tracking(NOON_UTC, null, null);

        assertSame(tracking, tracking.captureActivation(london(DAY.plusDays(1))));
    }
}
