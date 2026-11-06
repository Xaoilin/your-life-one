package com.yourlifeone.planner.tasks.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "tasks", name = "day_zone")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayZoneEntity {
    @Id
    private UUID userId;
    private String appTimeZone;
    private String prayerTimeZone;
    private Instant updatedAt;
}
