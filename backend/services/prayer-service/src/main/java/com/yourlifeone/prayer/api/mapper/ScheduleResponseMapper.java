package com.yourlifeone.prayer.api.mapper;

import com.yourlifeone.prayer.api.response.PrayerWindowResponse;
import com.yourlifeone.prayer.api.response.ScheduleResponse;
import com.yourlifeone.prayer.api.response.TimingResponse;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.PrayerWindow;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.domain.Timing;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps a timetable to its response: every timing in display order, plus each prayer's on-time window. */
@Component
public class ScheduleResponseMapper {
    private static final DateTimeFormatter CLOCK_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public ScheduleResponse toResponse(ScheduleDay day) {
        return ScheduleResponse.builder()
                .date(day.date())
                .hijriDate(day.hijriDate())
                .city(day.city())
                .country(day.country())
                .timezone(day.zone().getId())
                .method(ScheduleDay.METHOD_NAME)
                .times(timings(day))
                .windows(windows(day))
                .build();
    }

    private static List<TimingResponse> timings(ScheduleDay day) {
        var timings = new ArrayList<TimingResponse>();
        for (var timing : Timing.values()) {
            timings.add(TimingResponse.builder()
                    .name(timing.label())
                    .nameArabic(timing.arabic())
                    .time(CLOCK_TIME.format(day.times().get(timing)))
                    .type(timing.isPrayer() ? "prayer" : "event")
                    .build());
        }
        return timings;
    }

    /** Prayers whose window cannot be computed from the timetable are left out. */
    private static List<PrayerWindowResponse> windows(ScheduleDay day) {
        var windows = new ArrayList<PrayerWindowResponse>();
        for (var prayer : PrayerName.values()) {
            day.window(prayer).map(ScheduleResponseMapper::toResponse).ifPresent(windows::add);
        }
        return windows;
    }

    private static PrayerWindowResponse toResponse(PrayerWindow window) {
        return PrayerWindowResponse.builder()
                .prayer(window.prayer())
                .startsAt(window.startsAt())
                .deadlineName(window.deadline().label())
                .deadlineAt(window.deadlineAt())
                .build();
    }
}
