package com.yourlifeone.planner.clock.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.planner.clock.api.mapper.ClockMapper;
import com.yourlifeone.planner.clock.application.service.ClockService;
import com.yourlifeone.planner.shared.CommandHandler;
import com.yourlifeone.planner.shared.IdCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The clock writes the command queue performs; each answers with the whole workspace. */
@Configuration(proxyBeanMethods = false)
public class ClockCommandHandlers {
    public static final String SAVE_STOPWATCH = "clock.save-stopwatch";
    public static final String DELETE_STOPWATCH = "clock.delete-stopwatch";
    public static final String SAVE_TIMER = "clock.save-timer";
    public static final String DELETE_TIMER = "clock.delete-timer";

    @Bean
    WriteCommandHandler<SaveStopwatchCommand> saveStopwatchHandler(ClockService clock, ClockMapper mapper) {
        return CommandHandler.ok(SAVE_STOPWATCH, SaveStopwatchCommand.class, (user, command) -> mapper.toResponse(
                clock.saveStopwatch(user, mapper.toDomain(command.id(), command.stopwatch()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteStopwatchHandler(ClockService clock, ClockMapper mapper) {
        return CommandHandler.ok(DELETE_STOPWATCH, IdCommand.class, (user, command) -> mapper.toResponse(
                clock.deleteStopwatch(user, command.id())));
    }

    @Bean
    WriteCommandHandler<SaveTimerCommand> saveTimerHandler(ClockService clock, ClockMapper mapper) {
        return CommandHandler.ok(SAVE_TIMER, SaveTimerCommand.class, (user, command) -> mapper.toResponse(
                clock.saveTimer(user, mapper.toDomain(command.id(), command.timer()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteTimerHandler(ClockService clock, ClockMapper mapper) {
        return CommandHandler.ok(DELETE_TIMER, IdCommand.class, (user, command) -> mapper.toResponse(
                clock.deleteTimer(user, command.id())));
    }
}
