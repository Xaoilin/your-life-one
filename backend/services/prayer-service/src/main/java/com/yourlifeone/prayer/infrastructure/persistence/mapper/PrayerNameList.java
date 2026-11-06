package com.yourlifeone.prayer.infrastructure.persistence.mapper;

import com.yourlifeone.prayer.domain.PrayerName;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Stores a list of prayers in one text column as comma-separated labels. */
final class PrayerNameList {
    private static final String SEPARATOR = ",";

    private PrayerNameList() {
    }

    static String format(List<PrayerName> prayers) {
        return prayers.stream().map(PrayerName::label).collect(Collectors.joining(SEPARATOR));
    }

    static List<PrayerName> parse(String labels) {
        if (labels == null || labels.isBlank()) return List.of();
        return Arrays.stream(labels.split(SEPARATOR))
                .map(PrayerName::parseOrNull)
                .filter(Objects::nonNull)
                .toList();
    }
}
