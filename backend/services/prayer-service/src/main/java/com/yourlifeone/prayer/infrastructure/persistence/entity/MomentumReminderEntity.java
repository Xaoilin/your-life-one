package com.yourlifeone.prayer.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A user's momentum reminder preference for one pillar. */
@Entity
@Table(schema = "prayer", name = "momentum_reminder")
@IdClass(MomentumReminderId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentumReminderEntity {
    @Id
    private UUID userId;
    /** {@code learn} or {@code move}. */
    @Id
    private String pillar;
    private boolean enabled;
    /** Prayer labels, e.g. {@code Dhuhr}. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> afterPrayers;
    private LocalDate completedOn;
    private Instant updatedAt;
}
