package com.yourlifeone.prayer.infrastructure.theshia;

import com.yourlifeone.prayer.domain.Timing;
import com.yourlifeone.prayer.infrastructure.theshia.dto.TheShiaResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheShiaTimetableMapperTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final TheShiaTimetableMapper mapper = new TheShiaTimetableMapper();

    static String fixture(String name) throws IOException {
        try (var stream = TheShiaTimetableMapperTest.class.getResourceAsStream("/theshia/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void recordedPublicTimetablesPreserveMidnightAcrossSeasonsDstAndLocations() throws IOException {
        for (var sample : JSON.readTree(fixture("compatibility.json"))) {
            var date = LocalDate.parse(sample.path("date").asString());
            var response = JSON.readValue(fixture(sample.path("fixture").asString()), TheShiaResponse.class);
            var day = mapper.toScheduleDay(response, sample.path("city").asString(), "test", date, Instant.EPOCH);
            var oldTimes = sample.path("aladhan");
            for (var timing : Timing.values()) {
                var previous = LocalTime.parse(oldTimes.path(timing.label()).asString());
                int delta = day.times().get(timing).toSecondOfDay() / 60 - previous.toSecondOfDay() / 60;
                delta = Math.floorMod(delta + 720, 1440) - 720;
                int tolerance = timing == Timing.ASR ? 4 : timing == Timing.MIDNIGHT ? 0 : 1;
                assertTrue(Math.abs(delta) <= tolerance, sample.path("fixture") + " " + timing + " delta=" + delta);
            }
            assertEquals(date, day.date());
        }
    }

    @Test
    void rejectsWrongPresetDateTimezoneMissingAndUnorderedTimings() throws IOException {
        var valid = fixture("london-2026-11-06.json");
        for (var invalid : new String[] {
                valid.replace("\"Jafari\"", "\"Tehran\""),
                valid.replace("\"2026-11-06\"", "\"2026-11-07\""),
                valid.replace("Europe/London", "Nowhere/Land"),
                valid.replace("2026-11-06T17:53:00.000Z", "2026-11-06T04:00:00.000Z"),
                valid.replace("\"isha\": \"2026-11-06T17:53:00.000Z\"", "\"isha\": null")}) {
            assertThrows(InvalidTimetableException.class, () -> mapper.toScheduleDay(
                    JSON.readValue(invalid, TheShiaResponse.class), "London", "United Kingdom",
                    LocalDate.parse("2026-11-06"), Instant.EPOCH));
        }
    }
}
