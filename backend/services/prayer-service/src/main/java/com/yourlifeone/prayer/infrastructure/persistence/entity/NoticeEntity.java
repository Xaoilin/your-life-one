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

/** The receipt of a sent bounded notice: one per prayer opportunity, one per pillar of a momentum notice. */
@Entity
@Table(schema = "prayer", name = "notice")
@IdClass(NoticeId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeEntity {
    @Id
    private UUID userId;
    @Id
    private String key;
    /** {@code prayer-opportunity} or {@code momentum}. */
    private String kind;
    private LocalDate prayerDate;
    private String prayerName;
    /** {@code learn} or {@code move} for momentum; null otherwise. */
    private String pillar;
    private Instant firesAt;
    private Instant expiresAt;
    private Instant sentAt;
    private Instant snoozedUntil;
    private int snoozeCount;
    private Instant resentAt;
}
