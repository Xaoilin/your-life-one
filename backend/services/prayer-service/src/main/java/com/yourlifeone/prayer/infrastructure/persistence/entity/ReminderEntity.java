package com.yourlifeone.prayer.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The receipt of a sent deadline reminder: one per prayer per date. */
@Entity
@Table(schema = "prayer", name = "reminder")
@IdClass(ReminderId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEntity {
    @Id
    private UUID userId;
    @Id
    private LocalDate prayerDate;
    /** The prayer's label, e.g. {@code Fajr} (an ID column, so not converted). */
    @Id
    private String prayerName;
    private Instant deadlineAt;
    private Instant sentAt;
    private Instant snoozedUntil;
    private int snoozeCount;
    private Instant resentAt;
}
