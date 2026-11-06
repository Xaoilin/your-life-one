package com.yourlifeone.prayer.domain;

import java.time.Instant;
import lombok.Builder;

/** When a prayer starts and when it stops being on time. */
@Builder
public record PrayerWindow(PrayerName prayer, Instant startsAt, Timing deadline, Instant deadlineAt) {
}
