package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static com.yourlifeone.prayer.domain.Fixtures.DAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReminderKeysTest {
    @Test
    void everyKindOfKeyReadsBackAsItWasBuilt() {
        var firesAt = Instant.parse("2026-09-25T18:12:00Z");
        var momentum = ReminderKeys.of(ReminderKind.MOMENTUM, DAY, PrayerName.MAGHRIB, firesAt,
                List.of(Pillar.LEARN, Pillar.MOVE));

        assertEquals("momentum:2026-09-25:" + firesAt.toEpochMilli() + ":learn+move", momentum);
        assertEquals(Optional.of(ReminderKey.builder().kind(ReminderKind.MOMENTUM).date(DAY).firesAt(firesAt)
                .pillars(List.of(Pillar.LEARN, Pillar.MOVE)).build()), ReminderKeys.parse(momentum));
        assertEquals(Optional.of(ReminderKey.builder().kind(ReminderKind.DEADLINE).date(DAY).prayer(PrayerName.FAJR)
                .build()), ReminderKeys.parse("deadline:2026-09-25:Fajr"));
        assertEquals(ReminderKind.OPPORTUNITY, ReminderKeys.parse("opportunity:2026-09-25:Isha").orElseThrow().kind());
    }

    @Test
    void malformedKeysAreNotKeys() {
        for (var key : List.of("", "deadline:2026-09-25", "deadline:2026-13-01:Fajr", "opportunity:2026-09-25:Sunrise",
                "momentum:2026-09-25:12:move+learn", "momentum:2026-09-25:12:learn+learn", "momentum:2026-09-25:x:learn",
                "bounded:v1:momentum:2026-09-25:learn:Dhuhr:1")) {
            assertEquals(Optional.empty(), ReminderKeys.parse(key), key);
        }
    }
}
