package com.yourlifeone.planner.clock.infrastructure.persistence.entity;

import com.yourlifeone.planner.shared.UserScopedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(schema = "clock", name = "stopwatch")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopwatchEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String label;
    private long accumulatedMs;
    private Long startedAt;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<Long> laps;
    private int position;
    private Instant createdAt;
    private Instant updatedAt;
}
