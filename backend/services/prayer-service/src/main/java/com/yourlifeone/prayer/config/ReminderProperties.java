package com.yourlifeone.prayer.config;

import com.yourlifeone.prayer.domain.NoticePolicy;
import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.PrayerName;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reminder policy ({@code app.prayer.reminders.*}).
 *
 * @param opportunityWindow how long a prayer opportunity notice stays active after the prayer starts, at most
 * @param quietHoursStartHour hour (timetable zone) from which no momentum notice fires
 * @param quietHoursEndHour hour at which momentum notices may fire again
 * @param snooze how long one snooze lasts
 * @param defaultMomentumEnabled whether a pillar the app never configured prompts
 * @param defaultAnchors each pillar's anchor prayers until the app sends its own
 */
@ConfigurationProperties("app.prayer.reminders")
public record ReminderProperties(
        Duration opportunityWindow,
        int quietHoursStartHour,
        int quietHoursEndHour,
        Duration snooze,
        boolean defaultMomentumEnabled,
        Map<Pillar, List<PrayerName>> defaultAnchors) {
    private static final int HOURS_PER_DAY = 24;

    public ReminderProperties {
        requirePositive("opportunity-window", opportunityWindow);
        requirePositive("snooze", snooze);
        requireHour("quiet-hours-start-hour", quietHoursStartHour);
        requireHour("quiet-hours-end-hour", quietHoursEndHour);
        var anchors = new EnumMap<Pillar, List<PrayerName>>(Pillar.class);
        if (defaultAnchors != null) defaultAnchors.forEach((pillar, prayers) -> anchors.put(pillar, List.copyOf(prayers)));
        defaultAnchors = Map.copyOf(anchors);
    }

    public NoticePolicy policy() {
        return NoticePolicy.builder()
                .opportunityWindow(opportunityWindow)
                .quietHoursStartHour(quietHoursStartHour)
                .quietHoursEndHour(quietHoursEndHour)
                .build();
    }

    public List<PrayerName> defaultAnchors(Pillar pillar) {
        return defaultAnchors.getOrDefault(pillar, List.of());
    }

    private static void requirePositive(String name, Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("app.prayer.reminders." + name + " must be positive");
        }
    }

    private static void requireHour(String name, int hour) {
        if (hour < 0 || hour >= HOURS_PER_DAY) {
            throw new IllegalArgumentException("app.prayer.reminders." + name + " must be an hour from 0 to 23");
        }
    }
}
