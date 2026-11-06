package com.yourlifeone.planner.tasks.application.service;

import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.persistence.DatabaseUnavailableException;
import com.yourlifeone.planner.infrastructure.persistence.PlannerSchemas;
import com.yourlifeone.planner.tasks.api.command.RolloverCommand;
import com.yourlifeone.planner.tasks.api.command.TaskCommandHandlers;
import com.yourlifeone.planner.tasks.api.request.RolloverRequest;
import com.yourlifeone.planner.tasks.api.response.DayResponse;
import com.yourlifeone.planner.tasks.domain.DayZones;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the daily reset for every user whose time zones are known, through the command queue like any other
 * write, so the user's open tabs hear about it. The idempotency key names the day, so each user's reset
 * runs once per new day however often this checks. It has no user token to read the prayer service with, so
 * prayer rewards wait for the app's own daily reset or its reward sync.
 */
@Component
@RequiredArgsConstructor
public class RolloverScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RolloverScheduler.class);

    private final DayZoneService zones;
    private final WriteCommandGateway commands;
    private final PlannerSchemas schemas;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.planner.tasks.rollover-interval}",
            initialDelayString = "${app.planner.tasks.rollover-interval}")
    public void rolloverEveryUser() {
        try {
            schemas.ensureMigrated();
            zones.all().forEach(this::rollover);
        } catch (DatabaseUnavailableException exception) {
            logger.warn("Daily reset skipped: {}", exception.getMessage());
        }
    }

    private void rollover(UUID userId, DayZones dayZones) {
        var now = clock.instant();
        var prayerZone = dayZones.prayer() == null ? null : dayZones.prayer().getId();
        var key = String.join(":", "rollover", LocalDate.ofInstant(now, dayZones.app()).toString(),
                dayZones.prayer() == null ? "-" : LocalDate.ofInstant(now, dayZones.prayer()).toString(),
                dayZones.app().getId(), prayerZone == null ? "-" : prayerZone);
        try {
            commands.send(TaskCommandHandlers.ROLLOVER, userId, key,
                    new RolloverCommand(new RolloverRequest(dayZones.app().getId(), prayerZone), null, null),
                    DayResponse.class);
        } catch (RuntimeException exception) {
            logger.warn("Daily reset failed for a user: reason={}", exception.getMessage());
        }
    }
}
