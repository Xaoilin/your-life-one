package com.yourlifeone.planner.clock.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.planner.clock.api.command.ClockCommandHandlers;
import com.yourlifeone.planner.clock.api.command.SaveStopwatchCommand;
import com.yourlifeone.planner.clock.api.command.SaveTimerCommand;
import com.yourlifeone.planner.clock.api.mapper.ClockMapper;
import com.yourlifeone.planner.clock.api.request.StopwatchRequest;
import com.yourlifeone.planner.clock.api.request.TimerRequest;
import com.yourlifeone.planner.clock.api.response.ClockResponse;
import com.yourlifeone.planner.clock.application.service.ClockService;
import com.yourlifeone.planner.shared.IdCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The clock workspace: stopwatches with laps and countdown timers. IDs are chosen by the app. */
@RestController
@RequestMapping("/api/planner/v1/clock")
@RequiredArgsConstructor
public class ClockController {
    private final ClockService clock;
    private final ClockMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public ClockResponse workspace(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(clock.workspace(AuthenticatedUser.id(jwt)));
    }

    @PutMapping("/stopwatches/{id}")
    public ResponseEntity<ClockResponse> saveStopwatch(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody StopwatchRequest request) {
        return WriteCommandResponses.of(commands.send(ClockCommandHandlers.SAVE_STOPWATCH, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveStopwatchCommand(id, request), ClockResponse.class));
    }

    @DeleteMapping("/stopwatches/{id}")
    public ResponseEntity<ClockResponse> deleteStopwatch(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(ClockCommandHandlers.DELETE_STOPWATCH, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), ClockResponse.class));
    }

    @PutMapping("/timers/{id}")
    public ResponseEntity<ClockResponse> saveTimer(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody TimerRequest request) {
        return WriteCommandResponses.of(commands.send(ClockCommandHandlers.SAVE_TIMER, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveTimerCommand(id, request), ClockResponse.class));
    }

    @DeleteMapping("/timers/{id}")
    public ResponseEntity<ClockResponse> deleteTimer(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(ClockCommandHandlers.DELETE_TIMER, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), ClockResponse.class));
    }
}
