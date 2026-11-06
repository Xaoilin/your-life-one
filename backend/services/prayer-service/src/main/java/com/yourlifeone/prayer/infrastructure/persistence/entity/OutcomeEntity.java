package com.yourlifeone.prayer.infrastructure.persistence.entity;

import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import com.yourlifeone.prayer.domain.PrayerName;
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
@Table(schema = "prayer", name = "outcome")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeEntity {
    @Id
    private UUID id;
    private UUID userId;
    private LocalDate prayerDate;
    private PrayerName prayerName;
    private OutcomeStatus status;
    private OutcomeTiming timing;
    private Instant recordedAt;
    private String source;
    private String taskId;
    private boolean rewarded;
    private Instant deadlineAt;
}
