package com.yourlifeone.planner.clock.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.clock.domain.ClockWorkspace;
import com.yourlifeone.planner.clock.domain.CountdownTimer;
import com.yourlifeone.planner.clock.domain.Stopwatch;
import com.yourlifeone.planner.clock.infrastructure.persistence.entity.CounterEntity;
import com.yourlifeone.planner.clock.infrastructure.persistence.entity.StopwatchEntity;
import com.yourlifeone.planner.clock.infrastructure.persistence.entity.TimerEntity;
import com.yourlifeone.planner.clock.infrastructure.persistence.mapper.ClockPersistenceMapper;
import com.yourlifeone.planner.clock.infrastructure.persistence.repository.CounterRepository;
import com.yourlifeone.planner.clock.infrastructure.persistence.repository.StopwatchRepository;
import com.yourlifeone.planner.clock.infrastructure.persistence.repository.TimerRepository;
import com.yourlifeone.planner.shared.RecordIds;
import com.yourlifeone.planner.shared.Timestamps;
import com.yourlifeone.planner.shared.UserScopedId;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The clock workspace. Each write names one stopwatch or timer and answers with the whole workspace; a new
 * one goes last and takes the next number for its default name.
 */
@Service
@RequiredArgsConstructor
public class ClockService {
    private final StopwatchRepository stopwatches;
    private final TimerRepository timers;
    private final CounterRepository counters;
    private final ClockPersistenceMapper persistence;
    private final ClockRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ClockWorkspace workspace(UUID userId) {
        var counter = counters.findById(userId).orElse(new CounterEntity(userId, 1, 1));
        return new ClockWorkspace(
                stopwatches.findByUserIdOrderByPositionAscCreatedAtAsc(userId).stream().map(persistence::toDomain).toList(),
                timers.findByUserIdOrderByPositionAscCreatedAtAsc(userId).stream().map(persistence::toDomain).toList(),
                counter.getNextStopwatchNumber(), counter.getNextTimerNumber());
    }

    @Transactional
    public ClockWorkspace saveStopwatch(UUID userId, Stopwatch requested) {
        var now = Timestamps.now(clock);
        var existing = stopwatches.findById(new UserScopedId(userId, RecordIds.require(requested.id())));
        StopwatchEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
        } else {
            var counter = lockCounter(userId);
            var number = counter.getNextStopwatchNumber();
            counter.setNextStopwatchNumber(number + 1);
            entity = StopwatchEntity.builder().userId(userId).id(requested.id()).label("Stopwatch " + number)
                    .position(stopwatches.findByUserIdOrderByPositionAscCreatedAtAsc(userId).size()).createdAt(now)
                    .build();
        }
        var stopwatch = rules.stopwatch(requested, entity.getLabel());
        entity.setLabel(stopwatch.label());
        entity.setAccumulatedMs(stopwatch.accumulatedMs());
        entity.setStartedAt(stopwatch.startedAt());
        entity.setLaps(stopwatch.laps());
        entity.setUpdatedAt(now);
        stopwatches.save(entity);
        return workspace(userId);
    }

    @Transactional
    public ClockWorkspace deleteStopwatch(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!stopwatches.existsById(key)) throw notFound("stopwatch");
        stopwatches.deleteById(key);
        return workspace(userId);
    }

    @Transactional
    public ClockWorkspace saveTimer(UUID userId, CountdownTimer requested) {
        var now = Timestamps.now(clock);
        var existing = timers.findById(new UserScopedId(userId, RecordIds.require(requested.id())));
        TimerEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
        } else {
            var counter = lockCounter(userId);
            var number = counter.getNextTimerNumber();
            counter.setNextTimerNumber(number + 1);
            entity = TimerEntity.builder().userId(userId).id(requested.id()).label("Timer " + number)
                    .position(timers.findByUserIdOrderByPositionAscCreatedAtAsc(userId).size()).createdAt(now).build();
        }
        var timer = rules.timer(requested, entity.getLabel());
        entity.setLabel(timer.label());
        entity.setDurationMs(timer.durationMs());
        entity.setRemainingMs(timer.remainingMs());
        entity.setEndsAt(timer.endsAt());
        entity.setStatus(timer.status());
        entity.setSound(timer.sound());
        entity.setAlerting(timer.alerting());
        entity.setCompletedAt(timer.completedAt());
        entity.setUpdatedAt(now);
        timers.save(entity);
        return workspace(userId);
    }

    @Transactional
    public ClockWorkspace deleteTimer(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!timers.existsById(key)) throw notFound("timer");
        timers.deleteById(key);
        return workspace(userId);
    }

    private CounterEntity lockCounter(UUID userId) {
        counters.insertIfAbsent(userId);
        return counters.lockByUserId(userId).orElseThrow();
    }

    private static ApiException notFound(String kind) {
        return ApiException.notFound(kind + "_not_found", "This " + kind + " does not exist.");
    }
}
