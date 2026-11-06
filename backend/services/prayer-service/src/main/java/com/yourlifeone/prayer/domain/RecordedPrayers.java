package com.yourlifeone.prayer.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Which prayers have an outcome recorded, whatever its status; a recorded prayer gets no more reminders. */
public record RecordedPrayers(Set<String> keys) {
    public RecordedPrayers {
        keys = Set.copyOf(keys);
    }

    public static RecordedPrayers of(Collection<Outcome> outcomes) {
        return new RecordedPrayers(outcomes.stream()
                .map(outcome -> key(outcome.date(), outcome.prayer()))
                .collect(Collectors.toSet()));
    }

    public boolean contains(LocalDate date, PrayerName prayer) {
        return keys.contains(key(date, prayer));
    }

    public Set<PrayerName> onDate(LocalDate date) {
        var prayers = EnumSet.noneOf(PrayerName.class);
        for (var prayer : PrayerName.values()) {
            if (contains(date, prayer)) prayers.add(prayer);
        }
        return prayers;
    }

    /** The same key as {@link DueReminders#key}. */
    private static String key(LocalDate date, PrayerName prayer) {
        return date + "|" + prayer.label();
    }
}
