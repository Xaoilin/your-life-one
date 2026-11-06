package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.PrayerWindow;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.domain.Tracking;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stores a {@code missed} outcome for every tracked, unrecorded prayer whose window closed
 * yesterday or today. Existing outcomes are never touched.
 */
@Component
@RequiredArgsConstructor
class MissedPrayerRecorder {
    static final String SOURCE = "system";

    private final OutcomeService outcomes;

    void recordClosedWindows(UUID userId, Tracking tracking, Map<LocalDate, ScheduleDay> knownDays, LocalDate today,
            Instant now) {
        for (var date : List.of(today.minusDays(1), today)) {
            var day = knownDays.get(date);
            if (day != null) recordClosedWindows(userId, tracking, day, now);
        }
    }

    private void recordClosedWindows(UUID userId, Tracking tracking, ScheduleDay day, Instant now) {
        for (var prayer : PrayerName.values()) {
            day.window(prayer)
                    .filter(window -> !now.isBefore(window.deadlineAt()))
                    .filter(window -> tracking.isTracked(day, prayer, now))
                    .ifPresent(window -> outcomes.insertIfAbsent(userId, missed(day.date(), window, now)));
        }
    }

    private static Outcome missed(LocalDate date, PrayerWindow window, Instant now) {
        return Outcome.builder()
                .id(UUID.randomUUID())
                .date(date)
                .prayer(window.prayer())
                .status(OutcomeStatus.MISSED)
                .recordedAt(now)
                .source(SOURCE)
                .rewarded(false)
                .deadlineAt(window.deadlineAt())
                .build();
    }
}
