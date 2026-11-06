package com.yourlifeone.planner.gamification.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A prayer the prayer service recorded as prayed (its one-time reward receipt is set).
 *
 * @param prayer {@code Fajr}, {@code Dhuhr}, {@code Asr}, {@code Maghrib} or {@code Isha}
 * @param recordedAt when the user recorded it
 * @param taskId the prayer habit it was recorded from, if the app said
 */
public record PrayerOutcome(LocalDate date, String prayer, Instant recordedAt, String taskId) {
}
