package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.domain.Fixtures;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * London, 25 Sep 2026 (BST): Fajr 04:12Z, Sunrise 05:51Z, Dhuhr 11:52Z, Asr 15:08Z, Isha 19:18Z,
 * Midnight 23:52Z.
 */
@ExtendWith(MockitoExtension.class)
class OutcomeTimingPolicyTest {
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private UserScheduleService userSchedules;

    @InjectMocks
    private OutcomeTimingPolicy policy;

    private Instant check(PrayerName prayer, OutcomeStatus status, String now) {
        return policy.check(USER, DAY, prayer, status, Instant.parse(now));
    }

    private void timetableKnown() {
        when(userSchedules.knownDay(USER, DAY)).thenReturn(Optional.of(Fixtures.london(DAY)));
    }

    private static void assertRejected(String code, Runnable call) {
        var error = assertThrows(ApiException.class, call::run);
        assertEquals(code, error.code());
        assertEquals(409, error.status().value());
    }

    @Nested
    class WithTheDaysTimetable {
        @Test
        void acceptsACompletionInsideTheWindowAndReturnsItsDeadline() {
            timetableKnown();

            assertEquals(Instant.parse("2026-09-25T05:51:00Z"), check(PrayerName.FAJR, OutcomeStatus.ON_TIME, "2026-09-25T04:30:00Z"));
        }

        @Test
        void acceptsALateCompletionAfterTheWindow() {
            timetableKnown();

            assertEquals(Instant.parse("2026-09-25T15:08:00Z"), check(PrayerName.DHUHR, OutcomeStatus.LATE, "2026-09-25T20:00:00Z"));
        }

        @Test
        void acceptsAMissAfterTheWindowCloses() {
            timetableKnown();

            assertEquals(Instant.parse("2026-09-25T15:08:00Z"), check(PrayerName.DHUHR, OutcomeStatus.MISSED, "2026-09-25T15:08:00Z"));
        }

        @Test
        void rejectsFajrAndDhuhrInTheEarlyHoursBeforeTheyStart() {
            timetableKnown();

            assertRejected("prayer_not_started", () -> check(PrayerName.FAJR, OutcomeStatus.ON_TIME, "2026-09-25T01:10:00Z"));
            assertRejected("prayer_not_started", () -> check(PrayerName.DHUHR, OutcomeStatus.ON_TIME, "2026-09-25T01:10:00Z"));
        }

        @Test
        void rejectsAMissWhileThePrayerIsStillOnTime() {
            timetableKnown();

            assertRejected("prayer_window_open", () -> check(PrayerName.DHUHR, OutcomeStatus.MISSED, "2026-09-25T15:07:59Z"));
        }

        @Test
        void acceptsTheExactStartInstantAndRejectsOneSecondBefore() {
            timetableKnown();

            assertEquals(Instant.parse("2026-09-25T15:08:00Z"), check(PrayerName.DHUHR, OutcomeStatus.ON_TIME, "2026-09-25T11:52:00Z"));
            assertRejected("prayer_not_started", () -> check(PrayerName.DHUHR, OutcomeStatus.ON_TIME, "2026-09-25T11:51:59Z"));
        }

        @Test
        void allowsAsrFromDhuhrStartAndKeepsItsOwnDeadline() {
            timetableKnown();

            assertRejected("prayer_not_started", () -> check(PrayerName.ASR, OutcomeStatus.ON_TIME,
                    "2026-09-25T11:51:59.999Z"));
            assertEquals(Instant.parse("2026-09-25T18:12:00Z"), check(PrayerName.ASR, OutcomeStatus.ON_TIME,
                    "2026-09-25T11:52:00Z"));
            assertEquals(Instant.parse("2026-09-25T18:12:00Z"), check(PrayerName.ASR, OutcomeStatus.ON_TIME,
                    "2026-09-25T12:30:00Z"));
            assertEquals(Instant.parse("2026-09-25T18:12:00Z"), check(PrayerName.ASR, OutcomeStatus.ON_TIME,
                    "2026-09-25T15:08:00Z"));
        }

        @Test
        void allowsIshaFromMaghribStartAndKeepsItsOwnDeadline() {
            timetableKnown();

            assertRejected("prayer_not_started", () -> check(PrayerName.ISHA, OutcomeStatus.ON_TIME,
                    "2026-09-25T18:11:59.999Z"));
            assertEquals(Instant.parse("2026-09-25T23:52:00Z"), check(PrayerName.ISHA, OutcomeStatus.ON_TIME,
                    "2026-09-25T18:12:00Z"));
            assertEquals(Instant.parse("2026-09-25T23:52:00Z"), check(PrayerName.ISHA, OutcomeStatus.ON_TIME,
                    "2026-09-25T18:30:00Z"));
            assertEquals(Instant.parse("2026-09-25T23:52:00Z"), check(PrayerName.ISHA, OutcomeStatus.ON_TIME,
                    "2026-09-25T19:18:00Z"));
        }

        @Test
        void keepsIshaOnTimePastCalendarMidnightUntilTheIslamicMidnight() {
            timetableKnown();

            assertEquals(Instant.parse("2026-09-25T23:52:00Z"), check(PrayerName.ISHA, OutcomeStatus.ON_TIME, "2026-09-25T23:30:00Z"));
            assertRejected("prayer_window_open", () -> check(PrayerName.ISHA, OutcomeStatus.MISSED, "2026-09-25T23:51:00Z"));
        }
    }

    @Nested
    class WithoutTheDaysTimetable {
        @Test
        void acceptsAnyStatusForTodayOrEarlierAndStoresNoDeadline() {
            when(userSchedules.knownDay(USER, DAY)).thenReturn(Optional.empty());
            lenient().when(userSchedules.latest(USER)).thenReturn(Optional.of(Fixtures.london(DAY)));

            assertNull(check(PrayerName.FAJR, OutcomeStatus.MISSED, "2026-09-25T01:00:00Z"));
        }

        @Test
        void rejectsAFutureDateInTheUsersLatestTimeZone() {
            var tomorrow = DAY.plusDays(1);
            when(userSchedules.knownDay(USER, tomorrow)).thenReturn(Optional.empty());
            when(userSchedules.latest(USER)).thenReturn(Optional.of(Fixtures.london(DAY)));

            assertRejected("prayer_not_started", () -> policy.check(USER, tomorrow, PrayerName.FAJR, OutcomeStatus.ON_TIME,
                    Instant.parse("2026-09-25T22:30:00Z")));
        }

        @Test
        void usesTheUsersZoneForToday() {
            // 23:30Z on the 25th is already the 26th in London (BST), so the 26th is not in the future.
            var londonToday = LocalDate.parse("2026-09-26");
            when(userSchedules.knownDay(USER, londonToday)).thenReturn(Optional.empty());
            when(userSchedules.latest(USER)).thenReturn(Optional.of(Fixtures.london(DAY)));

            assertNull(policy.check(USER, londonToday, PrayerName.FAJR, OutcomeStatus.ON_TIME, Instant.parse("2026-09-25T23:30:00Z")));
        }

        @Test
        void fallsBackToUtcWithoutAnyKnownTimetable() {
            var tomorrow = DAY.plusDays(1);
            when(userSchedules.knownDay(USER, tomorrow)).thenReturn(Optional.empty());
            when(userSchedules.latest(USER)).thenReturn(Optional.empty());

            assertRejected("prayer_not_started", () -> policy.check(USER, tomorrow, PrayerName.FAJR, OutcomeStatus.ON_TIME,
                    Instant.parse("2026-09-25T23:30:00Z")));
        }
    }
}
