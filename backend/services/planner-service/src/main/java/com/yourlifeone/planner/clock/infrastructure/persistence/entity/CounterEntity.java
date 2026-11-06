package com.yourlifeone.planner.clock.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "clock", name = "counter")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterEntity {
    @Id
    private UUID userId;
    private int nextStopwatchNumber;
    private int nextTimerNumber;
}
