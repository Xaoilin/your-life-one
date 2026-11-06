package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.ScheduleDayRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Serves prayer timetables, fetching each location and date from the provider at most once. */
@Service
@RequiredArgsConstructor
public class ScheduleService {
    private static final int MAX_LOCATION_LENGTH = 100;

    private final ScheduleDayRepository scheduleDays;
    private final ScheduleSource source;
    private final PrayerPersistenceMapper mapper;
    private final Clock clock;

    /** The timetable for the current date in the location's own time zone. */
    public ScheduleDay today(String city, String country) {
        var location = trimmedCity(city);
        var countryName = trimmedCountry(country);
        var zone = knownZone(location, countryName);
        return forValidDate(location, countryName, LocalDate.ofInstant(clock.instant(), zone));
    }

    public ScheduleDay forDate(String city, String country, LocalDate date) {
        return forValidDate(trimmedCity(city), trimmedCountry(country), date);
    }

    /** The location's time zone, learnt from any cached timetable or from today's UTC timetable. */
    private ZoneId knownZone(String city, String country) {
        return scheduleDays
                .findFirstByCityIgnoreCaseAndCountryIgnoreCaseAndMethodOrderByScheduleDateDesc(
                        city, country, ScheduleDay.METHOD_ID)
                .map(entity -> ZoneId.of(entity.getTimeZone()))
                .orElseGet(() -> forValidDate(city, country, LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC))
                        .zone());
    }

    private ScheduleDay forValidDate(String city, String country, LocalDate date) {
        return scheduleDays.findForDate(city, country, ScheduleDay.METHOD_ID, date)
                .map(mapper::toDomain)
                .orElseGet(() -> fetchAndCache(city, country, date));
    }

    private ScheduleDay fetchAndCache(String city, String country, LocalDate date) {
        var fetched = source.fetch(city, country, date);
        scheduleDays.insertIfAbsent(mapper.toEntity(fetched, UUID.randomUUID()));
        return scheduleDays.findForDate(city, country, ScheduleDay.METHOD_ID, date)
                .map(mapper::toDomain)
                .orElseThrow();
    }

    private static String trimmedCity(String city) {
        return requireLocationPart("city", city);
    }

    private static String trimmedCountry(String country) {
        return requireLocationPart("country", country);
    }

    private static String requireLocationPart(String name, String value) {
        var trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LOCATION_LENGTH) {
            throw ApiException.badRequest("invalid_location", name + " must be 1-" + MAX_LOCATION_LENGTH + " characters.");
        }
        return trimmed;
    }
}
