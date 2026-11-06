package com.yourlifeone.planner.gamification.infrastructure.persistence.entity;

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

@Entity
@Table(schema = "gamification", name = "prayer_reward")
@IdClass(PrayerRewardId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrayerRewardEntity {
    @Id
    private UUID userId;
    @Id
    private LocalDate prayerDate;
    @Id
    private String prayerName;
    private String source;
    private String taskId;
    private String logId;
    private int xpEarned;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> newBadges;
    private int level;
    private boolean leveledUp;
    private int currentStreak;
    private boolean streakMilestone;
    private boolean completedTask;
    private Instant recordedAt;
    private Instant grantedAt;
}
