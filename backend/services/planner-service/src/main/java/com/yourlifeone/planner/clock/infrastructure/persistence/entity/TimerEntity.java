package com.yourlifeone.planner.clock.infrastructure.persistence.entity;

import com.yourlifeone.planner.shared.UserScopedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "clock", name = "timer")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String label;
    private long durationMs;
    private long remainingMs;
    private Long endsAt;
    private String status;
    private String sound;
    private boolean alerting;
    private Instant completedAt;
    private int position;
    private Instant createdAt;
    private Instant updatedAt;
}
