package com.yourlifeone.prayer.domain;

import java.time.Duration;
import lombok.Builder;

/**
 * The configurable limits of bounded notices.
 *
 * @param opportunityWindow how long after a prayer starts its opportunity notice stays active, at most
 * @param quietHoursStartHour the hour (0-23, timetable zone) from which momentum notices stay quiet
 * @param quietHoursEndHour the hour at which the quiet ends; before the start hour means overnight
 */
@Builder
public record NoticePolicy(Duration opportunityWindow, int quietHoursStartHour, int quietHoursEndHour) {
    /** Whether the hour falls inside the non-prayer quiet hours. */
    public boolean isQuietHour(int hour) {
        if (quietHoursStartHour > quietHoursEndHour) {
            return hour >= quietHoursStartHour || hour < quietHoursEndHour;
        }
        return hour >= quietHoursStartHour && hour < quietHoursEndHour;
    }
}
