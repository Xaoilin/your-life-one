package com.yourlifeone.planner.tasks.infrastructure.persistence.entity;

import com.yourlifeone.planner.shared.UserScopedId;
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

@Entity
@Table(schema = "tasks", name = "task")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private boolean completed;
    private Instant completedAt;
    private LocalDate completedLocalDate;
    private String completionTimeZone;
    private LocalDate dueDate;
    private String recurrence;
    private LocalDate lastReset;
    private String prayerName;
    private String goalTag;
    private String emoji;
    private String projectId;
    private String workflowState;
    private String blockedReason;
    private Integer boardOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
