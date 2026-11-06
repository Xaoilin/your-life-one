package com.yourlifeone.prayer.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The keys the app uses to name a reminder, e.g. to snooze it: {@code deadline:<date>:<Prayer>},
 * {@code opportunity:<date>:<Prayer>} and {@code momentum:<date>:<firesAtEpochMs>:<pillar>[+<pillar>]}.
 */
public final class ReminderKeys {
    private static final String SEPARATOR = ":";
    private static final String PILLAR_SEPARATOR = "+";
    private static final String DEADLINE = "deadline";
    private static final String OPPORTUNITY = "opportunity";
    private static final String MOMENTUM = "momentum";
    private static final int PRAYER_KEY_PARTS = 3;
    private static final int MOMENTUM_KEY_PARTS = 4;
    private static final Pattern EPOCH_MILLIS = Pattern.compile("\\d{1,15}");

    private ReminderKeys() {
    }

    public static String of(ReminderKind kind, LocalDate date, PrayerName prayer, Instant firesAt, List<Pillar> pillars) {
        return switch (kind) {
            case DEADLINE -> String.join(SEPARATOR, DEADLINE, date.toString(), prayer.label());
            case OPPORTUNITY -> String.join(SEPARATOR, OPPORTUNITY, date.toString(), prayer.label());
            case MOMENTUM -> String.join(SEPARATOR, MOMENTUM, date.toString(), String.valueOf(firesAt.toEpochMilli()),
                    pillars.stream().map(Pillar::label).collect(Collectors.joining(PILLAR_SEPARATOR)));
        };
    }

    /** The key's parts; empty when it is not a well-formed key. */
    public static Optional<ReminderKey> parse(String key) {
        if (key == null) return Optional.empty();
        var parts = key.split(SEPARATOR, -1);
        try {
            return switch (parts[0]) {
                case DEADLINE -> prayerKey(ReminderKind.DEADLINE, parts);
                case OPPORTUNITY -> prayerKey(ReminderKind.OPPORTUNITY, parts);
                case MOMENTUM -> momentumKey(parts);
                default -> Optional.empty();
            };
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<ReminderKey> prayerKey(ReminderKind kind, String[] parts) {
        if (parts.length != PRAYER_KEY_PARTS) return Optional.empty();
        var prayer = PrayerName.parseOrNull(parts[2]);
        if (prayer == null) return Optional.empty();
        return Optional.of(ReminderKey.builder().kind(kind).date(LocalDate.parse(parts[1])).prayer(prayer).build());
    }

    private static Optional<ReminderKey> momentumKey(String[] parts) {
        if (parts.length != MOMENTUM_KEY_PARTS || !EPOCH_MILLIS.matcher(parts[2]).matches()) return Optional.empty();
        var pillars = pillars(parts[3]);
        if (pillars.isEmpty()) return Optional.empty();
        return Optional.of(ReminderKey.builder()
                .kind(ReminderKind.MOMENTUM)
                .date(LocalDate.parse(parts[1]))
                .firesAt(Instant.ofEpochMilli(Long.parseLong(parts[2])))
                .pillars(pillars)
                .build());
    }

    /** Distinct pillars in pillar order, as keys are built; anything else is not a key. */
    private static List<Pillar> pillars(String joined) {
        var pillars = new ArrayList<Pillar>();
        for (var label : joined.split(Pattern.quote(PILLAR_SEPARATOR), -1)) pillars.add(Pillar.fromLabel(label));
        var canonical = pillars.stream().distinct().sorted(Comparator.naturalOrder()).toList();
        return canonical.equals(pillars) ? pillars : List.of();
    }
}
