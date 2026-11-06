package com.yourlifeone.prayer.infrastructure.theshia;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.domain.Timing;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TheShiaScheduleSourceTest {
    private static final LocalDate DATE = LocalDate.parse("2026-11-06");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneOffset.UTC);
    private static final String LOCATION_URL = "https://geo.test/v1/search?name=Bedford%2C%20United%20Kingdom"
            + "&count=10&language=en&format=json";
    private static final String TIMES_URL = "https://theshia.test/api/v1/prayer-times?lat=52.13459&lng=-0.46632"
            + "&tz=Europe%2FLondon&date=2026-11-06&method=Jafari";
    private static final String HIJRI_URL = "https://theshia.test/api/v1/hijri?date=2026-11-06&calendar=moonsighting";
    private MockRestServiceServer geocoding;
    private MockRestServiceServer provider;
    private TheShiaScheduleSource source;

    @BeforeEach
    void setUp() {
        var geoBuilder = RestClient.builder().baseUrl("https://geo.test/v1");
        geocoding = MockRestServiceServer.bindTo(geoBuilder).build();
        var timesBuilder = RestClient.builder().baseUrl("https://theshia.test/api/v1");
        provider = MockRestServiceServer.bindTo(timesBuilder).build();
        source = TheShiaScheduleSource.builder().provider(timesBuilder.build())
                .locations(new PrayerLocationResolver(geoBuilder.build()))
                .mapper(new TheShiaTimetableMapper()).clock(CLOCK).build();
    }

    private void locationResponse() throws IOException {
        geocoding.expect(requestTo(LOCATION_URL)).andRespond(withSuccess(
                TheShiaTimetableMapperTest.fixture("geocoding-bedford.json"), MediaType.APPLICATION_JSON));
    }

    private void timetableResponse() throws IOException {
        provider.expect(requestTo(TIMES_URL)).andRespond(withSuccess(
                TheShiaTimetableMapperTest.fixture("bedford-2026-11-06.json"), MediaType.APPLICATION_JSON));
    }

    @Test
    void resolvesCountryQualifiedBedfordAndFetchesJafariUtcTimingsAndHijriDate() throws IOException {
        locationResponse();
        timetableResponse();
        provider.expect(requestTo(HIJRI_URL)).andRespond(withSuccess(
                TheShiaTimetableMapperTest.fixture("hijri-2026-11-06.json"), MediaType.APPLICATION_JSON));
        var day = source.fetch("Bedford", "United Kingdom", DATE);
        assertEquals(LocalTime.of(13, 57), day.times().get(Timing.ASR));
        assertEquals(LocalTime.of(23, 45), day.times().get(Timing.MIDNIGHT));
        assertEquals("Europe/London", day.zone().getId());
        assertEquals("25 Jumada al-Awwal 1448 AH", day.hijriDate());
        assertEquals(CLOCK.instant(), day.fetchedAt());
        geocoding.verify();
        provider.verify();
    }

    @Test
    void unknownCityDoesNotUseAnUnrelatedDefaultLocation() {
        geocoding.expect(requestTo(LOCATION_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var error = assertThrows(ApiException.class, () -> source.fetch("Bedford", "United Kingdom", DATE));
        assertEquals("location_not_found", error.code());
        assertEquals(400, error.status().value());
        provider.verify();
    }

    @Test
    void providerFailuresAreUnavailableAndDoNotReturnAnUnvalidatedTimetable() throws IOException {
        locationResponse();
        provider.expect(requestTo(TIMES_URL)).andRespond(withServerError());
        var error = assertThrows(ApiException.class, () -> source.fetch("Bedford", "United Kingdom", DATE));
        assertEquals("schedule_unavailable", error.code());
        assertEquals(503, error.status().value());
        provider.verify();
    }

    @Test
    void optionalHijriFailureKeepsTheValidatedTimetable() throws IOException {
        locationResponse();
        timetableResponse();
        provider.expect(requestTo(HIJRI_URL)).andRespond(withServerError());
        var day = source.fetch("Bedford", "United Kingdom", DATE);
        assertEquals("", day.hijriDate());
        assertEquals(LocalTime.of(23, 45), day.times().get(Timing.MIDNIGHT));
        provider.verify();
    }
}
