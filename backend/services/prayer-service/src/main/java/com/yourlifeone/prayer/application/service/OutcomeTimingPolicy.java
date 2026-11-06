package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.PrayerWindow;
import com.yourlifeone.prayer.domain.ScheduleDay;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks that an outcome fits the prayer's time: Asr and Isha can be recorded from their paired
 * anchor prayer's start, and no prayer can be marked missed while it is still on time.
 */
@Component
@RequiredArgsConstructor
class OutcomeTimingPolicy {
    private final UserScheduleService userSchedules;

    /** @return the on-time deadline to store with the outcome, or null when the timetable is unknown */
    Instant check(UUID userId, LocalDate date, PrayerName prayer, OutcomeStatus status, Instant now) {
        var day = userSchedules.knownDay(userId, date);
        if (day.isEmpty()) {
            rejectFutureDate(userId, date, prayer, now);
            return null;
        }
        var window = day.get().window(prayer);
        if (window.isEmpty()) return null;
        rejectOutsideWindow(window.get(), recordableFrom(day.get(), prayer, window.get()), status, now);
        return window.get().deadlineAt();
    }

    /** Without that day's timetable, only dates after today (in the user's latest time zone) are rejected. */
    private void rejectFutureDate(UUID userId, LocalDate date, PrayerName prayer, Instant now) {
        var zone = userSchedules.latest(userId).map(ScheduleDay::zone).orElse(ZoneOffset.UTC);
        if (date.isAfter(LocalDate.ofInstant(now, zone))) throw notStarted(prayer);
    }

    private static Instant recordableFrom(ScheduleDay day, PrayerName prayer, PrayerWindow window) {
        var anchor = switch (prayer) {
            case ASR -> PrayerName.DHUHR;
            case ISHA -> PrayerName.MAGHRIB;
            default -> prayer;
        };
        return day.window(anchor).map(PrayerWindow::startsAt).orElse(window.startsAt());
    }

    private static void rejectOutsideWindow(PrayerWindow window, Instant recordableFrom, OutcomeStatus status, Instant now) {
        if (now.isBefore(recordableFrom)) throw notStarted(window.prayer());
        if (status == OutcomeStatus.MISSED && now.isBefore(window.deadlineAt())) {
            throw ApiException.conflict("prayer_window_open",
                    window.prayer().label() + " is still on time until " + window.deadlineAt() + ".");
        }
    }

    private static ApiException notStarted(PrayerName prayer) {
        return ApiException.conflict("prayer_not_started", prayer.label() + " has not started yet.");
    }
}
