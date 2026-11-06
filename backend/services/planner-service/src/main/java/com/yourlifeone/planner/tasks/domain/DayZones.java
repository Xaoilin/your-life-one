package com.yourlifeone.planner.tasks.domain;

import java.time.ZoneId;

/**
 * The time zones a user's days are counted in.
 *
 * @param app the app's time zone: daily habits, completions and the streak use its dates
 * @param prayer the prayer timetable's time zone: prayer habits reopen on its dates; null while unknown
 */
public record DayZones(ZoneId app, ZoneId prayer) {
}
