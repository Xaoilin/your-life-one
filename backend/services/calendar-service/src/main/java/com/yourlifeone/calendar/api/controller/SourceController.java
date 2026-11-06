package com.yourlifeone.calendar.api.controller;

import com.yourlifeone.calendar.api.command.CreateSourceHandler;
import com.yourlifeone.calendar.api.command.DeleteSourceCommand;
import com.yourlifeone.calendar.api.command.DeleteSourceHandler;
import com.yourlifeone.calendar.api.command.UpdateSourceCommand;
import com.yourlifeone.calendar.api.command.UpdateSourceHandler;
import com.yourlifeone.calendar.api.request.CreateSourceRequest;
import com.yourlifeone.calendar.api.request.UpdateSourceRequest;
import com.yourlifeone.calendar.api.response.SourceResponse;
import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Calendars (sources). Every write goes through the calendar command queue. */
@RestController
@RequestMapping("/api/calendar/v1/sources")
@RequiredArgsConstructor
public class SourceController {
    private final WriteCommandGateway commands;

    @PostMapping
    public ResponseEntity<SourceResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateSourceRequest request) {
        return WriteCommandResponses.of(commands.send(CreateSourceHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, SourceResponse.class));
    }

    @PatchMapping("/{sourceId}")
    public ResponseEntity<SourceResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String sourceId,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody UpdateSourceRequest request) {
        return WriteCommandResponses.of(commands.send(UpdateSourceHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new UpdateSourceCommand(sourceId, request), SourceResponse.class));
    }

    @DeleteMapping("/{sourceId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String sourceId,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(DeleteSourceHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new DeleteSourceCommand(sourceId), Void.class));
    }
}
