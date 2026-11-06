package com.yourlifeone.prayer.infrastructure.theshia;

import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.domain.TimetableValidator;
import com.yourlifeone.prayer.domain.Timing;
import com.yourlifeone.prayer.infrastructure.theshia.dto.TheShiaResponse;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import org.springframework.stereotype.Component;

/** Maps TheShia's explicit Ja'fari preset while preserving the existing midnight deadline rule. */
@Component
public class TheShiaTimetableMapper {
    public ScheduleDay toScheduleDay(
            TheShiaResponse response, String city, String country, LocalDate date, Instant fetchedAt) {
        if (response == null || response.meta() == null || !"Jafari".equals(response.meta().method())) {
            throw new InvalidTimetableException("missing Ja'fari timetable metadata");
        }
        if (response.days() == null || response.days().size() != 1 || response.days().getFirst() == null) {
            throw new InvalidTimetableException("expected one timetable day");
        }
        var day = response.days().getFirst();
        if (!date.equals(day.date())) throw new InvalidTimetableException("wrong timetable date");
        var zone = parseZone(response.meta().tz());
        var times = new EnumMap<Timing, LocalTime>(Timing.class);
        times.put(Timing.FAJR, localTime(day.fajr(), date, zone));
        times.put(Timing.SUNRISE, localTime(day.sunrise(), date, zone));
        times.put(Timing.DHUHR, localTime(day.dhuhr(), date, zone));
        times.put(Timing.ASR, localTime(day.asr(), date, zone));
        times.put(Timing.SUNSET, localTime(day.sunset(), date, zone));
        times.put(Timing.MAGHRIB, localTime(day.maghrib(), date, zone));
        times.put(Timing.ISHA, localTime(day.isha(), date, zone));
        times.put(Timing.MIDNIGHT, standardMidnight(times.get(Timing.SUNSET), times.get(Timing.SUNRISE)));
        if (!TimetableValidator.isValid(times)) throw new InvalidTimetableException("timings out of order");
        return ScheduleDay.builder().city(city).country(country).date(date).zone(zone)
                .hijriDate("").times(times).fetchedAt(fetchedAt).build();
    }

    /**
     * Existing AlAdhan STANDARD behavior: halfway from sunset to the same day's sunrise clock time
     * on the following day. Work in local clock minutes, including on DST dates, and truncate seconds.
     * Do not use TheShia's Maghrib-to-next-Fajr midnight: that would shorten the Isha window.
     */
    static LocalTime standardMidnight(LocalTime sunset, LocalTime sunrise) {
        long sunsetMinutes = sunset.toSecondOfDay() / 60;
        long sunriseMinutes = sunrise.toSecondOfDay() / 60 + 24 * 60;
        long midpoint = (sunsetMinutes + sunriseMinutes) / 2;
        return LocalTime.ofSecondOfDay(midpoint % (24 * 60) * 60);
    }

    private static ZoneId parseZone(String value) {
        try {
            return ZoneId.of(value == null ? "" : value);
        } catch (DateTimeException exception) {
            throw new InvalidTimetableException("invalid timezone");
        }
    }

    private static LocalTime localTime(Instant value, LocalDate date, ZoneId zone) {
        if (value == null) throw new InvalidTimetableException("missing timing");
        var local = value.atZone(zone);
        if (!date.equals(local.toLocalDate())) throw new InvalidTimetableException("timing on wrong local date");
        return local.toLocalTime().withSecond(0).withNano(0);
    }
}
