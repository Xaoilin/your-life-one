package com.yourlifeone.prayer.infrastructure.theshia;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.application.port.ScheduleSource;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.infrastructure.theshia.dto.GeocodingLocation;
import com.yourlifeone.prayer.infrastructure.theshia.dto.TheShiaHijriResponse;
import com.yourlifeone.prayer.infrastructure.theshia.dto.TheShiaResponse;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Keyless direct provider access; only public location/date data leaves this service. */
public class TheShiaScheduleSource implements ScheduleSource {
    private static final Logger logger = LoggerFactory.getLogger(TheShiaScheduleSource.class);
    private final RestClient provider;
    private final PrayerLocationResolver locations;
    private final TheShiaTimetableMapper mapper;
    private final Clock clock;

    @Builder
    public TheShiaScheduleSource(RestClient provider, PrayerLocationResolver locations,
            TheShiaTimetableMapper mapper, Clock clock) {
        this.provider = provider;
        this.locations = locations;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public ScheduleDay fetch(String city, String country, LocalDate date) {
        try {
            var location = locations.resolve(city, country);
            validateLocation(location);
            var response = provider.get()
                    .uri(uri -> uri.path("/prayer-times")
                            .queryParam("lat", location.latitude())
                            .queryParam("lng", location.longitude())
                            .queryParam("tz", "{zone}")
                            .queryParam("date", date)
                            .queryParam("method", "Jafari")
                            .build(location.timezone()))
                    .retrieve().body(TheShiaResponse.class);
            var day = mapper.toScheduleDay(response, city, country, date, clock.instant());
            if (!day.zone().getId().equals(location.timezone())) {
                throw new InvalidTimetableException("provider timezone does not match location");
            }
            return day.toBuilder().hijriDate(hijriDate(date)).build();
        } catch (RestClientException | InvalidTimetableException exception) {
            logger.warn("Prayer timetable provider unavailable ({})", exception.getClass().getSimpleName());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "schedule_unavailable",
                    "Prayer times are unavailable right now; try again shortly.", "Prayer provider failed", exception);
        }
    }

    private static void validateLocation(GeocodingLocation location) {
        if (location.latitude() == null || !Double.isFinite(location.latitude())
                || Math.abs(location.latitude()) > 90 || location.longitude() == null
                || !Double.isFinite(location.longitude()) || Math.abs(location.longitude()) > 180) {
            throw new InvalidTimetableException("invalid location coordinates");
        }
        try {
            ZoneId.of(location.timezone() == null ? "" : location.timezone());
        } catch (DateTimeException exception) {
            throw new InvalidTimetableException("invalid location timezone");
        }
    }

    /** A missing display date never discards an otherwise valid prayer timetable. */
    private String hijriDate(LocalDate date) {
        try {
            var response = provider.get().uri(uri -> uri.path("/hijri")
                    .queryParam("date", date).queryParam("calendar", "moonsighting").build())
                    .retrieve().body(TheShiaHijriResponse.class);
            if (response == null || !date.equals(response.gregorian()) || response.formatted() == null) return "";
            var formatted = response.formatted().get("en");
            return formatted == null ? "" : formatted;
        } catch (RestClientException exception) {
            logger.warn("Prayer Hijri display date unavailable ({})", exception.getClass().getSimpleName());
            return "";
        }
    }
}
