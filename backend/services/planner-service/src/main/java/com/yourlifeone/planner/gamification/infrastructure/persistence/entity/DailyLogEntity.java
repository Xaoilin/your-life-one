package com.yourlifeone.planner.gamification.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "gamification", name = "daily_log")
@IdClass(DailyLogId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogEntity {
    @Id
    private UUID userId;
    @Id
    private LocalDate day;
    @Id
    private String taskId;
}
