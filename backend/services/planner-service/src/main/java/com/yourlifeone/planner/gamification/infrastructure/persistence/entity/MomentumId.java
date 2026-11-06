package com.yourlifeone.planner.gamification.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MomentumId implements Serializable {
    private UUID userId;
    private String pillar;
}
