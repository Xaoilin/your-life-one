package com.yourlifeone.prayer.domain;

import java.time.LocalTime;
import java.util.Map;

/** Rejects provider timetables whose timings are missing or out of their natural order. */
public final class TimetableValidator {
    private static final int MINUTES_PER_DAY = 24 * 60;

    private TimetableValidator() {
    }

    /**
     * The day runs Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha, then the Islamic midnight,
     * which may fall after calendar midnight but always before the next Fajr.
     */
    public static boolean isValid(Map<Timing, LocalTime> times) {
        if (!hasEveryTiming(times)) return false;

        int fajr = minutesOf(times, Timing.FAJR);
        int sunrise = minutesOf(times, Timing.SUNRISE);
        int dhuhr = minutesOf(times, Timing.DHUHR);
        int asr = minutesOf(times, Timing.ASR);
        int sunset = minutesOf(times, Timing.SUNSET);
        int maghrib = minutesOf(times, Timing.MAGHRIB);
        int isha = minutesOf(times, Timing.ISHA);
        int midnight = minutesAfterIsha(minutesOf(times, Timing.MIDNIGHT), isha);

        boolean daytimeInOrder = fajr < sunrise && sunrise < dhuhr && dhuhr <= asr && asr < sunset;
        boolean eveningInOrder = sunset <= maghrib && maghrib <= isha && isha < midnight;
        boolean midnightBeforeNextFajr = midnight < fajr + MINUTES_PER_DAY;
        return daytimeInOrder && eveningInOrder && midnightBeforeNextFajr;
    }

    private static boolean hasEveryTiming(Map<Timing, LocalTime> times) {
        for (var timing : Timing.values()) {
            if (times.get(timing) == null) return false;
        }
        return true;
    }

    /** A midnight at or before Isha's clock time belongs to the next calendar day. */
    private static int minutesAfterIsha(int midnight, int isha) {
        return midnight <= isha ? midnight + MINUTES_PER_DAY : midnight;
    }

    private static int minutesOf(Map<Timing, LocalTime> times, Timing timing) {
        return times.get(timing).toSecondOfDay() / 60;
    }
}
