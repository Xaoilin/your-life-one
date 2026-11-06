package com.yourlifeone.planner.clock.infrastructure.persistence.mapper;

import com.yourlifeone.planner.clock.domain.CountdownTimer;
import com.yourlifeone.planner.clock.domain.Stopwatch;
import com.yourlifeone.planner.clock.infrastructure.persistence.entity.StopwatchEntity;
import com.yourlifeone.planner.clock.infrastructure.persistence.entity.TimerEntity;
import org.mapstruct.Mapper;

@Mapper
public interface ClockPersistenceMapper {
    Stopwatch toDomain(StopwatchEntity entity);

    CountdownTimer toDomain(TimerEntity entity);
}
