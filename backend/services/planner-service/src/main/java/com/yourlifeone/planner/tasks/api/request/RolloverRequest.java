package com.yourlifeone.planner.tasks.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The time zones the user's days are counted in, which the service remembers for its own daily reset.
 *
 * @param appTimeZone the app's time zone
 * @param prayerTimeZone the prayer timetable's time zone (the app's when prayer times are off); null while
 *     it is not known yet, which keeps the one reported before
 */
public record RolloverRequest(@NotBlank @Size(max = 64) String appTimeZone, @Size(max = 64) String prayerTimeZone) {
}
