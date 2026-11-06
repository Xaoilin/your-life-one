package com.yourlifeone.planner.gamification.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(schema = "gamification", name = "profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileEntity {
    @Id
    private UUID userId;
    private long totalXp;
    private int level;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastCompletionDate;
    private int totalTasksCompleted;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> badges;
    private Instant updatedAt;
}
