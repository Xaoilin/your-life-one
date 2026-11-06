package com.yourlifeone.planner.gamification.api.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Asks the planner to bring prayer XP in line with the prayer service.
 *
 * @param date the prayer the app just recorded, whose reward the response reports; may be null
 * @param prayer its name, e.g. {@code Fajr}; may be null
 */
public record PrayerRewardSyncRequest(LocalDate date, @Size(max = 16) String prayer) {
}
