package com.yourlifeone.planner.gamification.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "gamification", name = "habit_tally")
@IdClass(HabitTallyId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitTallyEntity {
    @Id
    private UUID userId;
    @Id
    private String taskId;
    private int count;
}
