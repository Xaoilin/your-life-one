package com.yourlifeone.planner.gamification.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "gamification", name = "momentum")
@IdClass(MomentumId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentumEntity {
    @Id
    private UUID userId;
    @Id
    private String pillar;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> state;
    /** Saving from an out-of-date version fails instead of overwriting newer progress. */
    @Version
    private Integer version;
    private Instant updatedAt;
}
