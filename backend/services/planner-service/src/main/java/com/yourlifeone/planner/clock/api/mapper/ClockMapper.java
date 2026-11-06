package com.yourlifeone.planner.clock.api.mapper;

import com.yourlifeone.planner.clock.api.request.StopwatchRequest;
import com.yourlifeone.planner.clock.api.request.TimerRequest;
import com.yourlifeone.planner.clock.api.response.ClockResponse;
import com.yourlifeone.planner.clock.domain.ClockWorkspace;
import com.yourlifeone.planner.clock.domain.CountdownTimer;
import com.yourlifeone.planner.clock.domain.Stopwatch;
import org.mapstruct.Mapper;

@Mapper
public interface ClockMapper {
    ClockResponse toResponse(ClockWorkspace workspace);

    Stopwatch toDomain(String id, StopwatchRequest request);

    CountdownTimer toDomain(String id, TimerRequest request);
}
