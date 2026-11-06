package com.yourlifeone.prayer.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "prayer", name = "profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrayerProfileEntity {
    @Id
    private UUID userId;
    private boolean enabled;
    private boolean reminderEnabled;
    private int reminderMinutes;
    private Instant trackingStartedAt;
    private LocalDate activationDate;
    /** Comma-separated prayer names, e.g. {@code Dhuhr,Asr,Maghrib,Isha}. */
    private String activationPrayers;
    private Instant importedAt;
    private Instant updatedAt;
}
