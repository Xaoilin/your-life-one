package com.yourlifeone.calendar.api.controller;

import com.yourlifeone.calendar.api.command.CreateEventHandler;
import com.yourlifeone.calendar.api.command.DeleteEventCommand;
import com.yourlifeone.calendar.api.command.DeleteEventHandler;
import com.yourlifeone.calendar.api.command.UpdateEventCommand;
import com.yourlifeone.calendar.api.command.UpdateEventHandler;
import com.yourlifeone.calendar.api.request.EventRequest;
import com.yourlifeone.calendar.api.response.EventResponse;
import com.yourlifeone.calendar.application.service.EventService;
import com.yourlifeone.calendar.application.service.GoogleAccessTokens;
import com.yourlifeone.calendar.domain.CalendarAccount;
import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Event writes go through the calendar command queue. A write to a Google calendar first prepares
 * the account's Google access token, so a credential problem is reported before anything is queued.
 */
@RestController
@RequestMapping("/api/calendar/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService events;
    private final GoogleAccessTokens googleTokens;
    private final WriteCommandGateway commands;

    @PostMapping
    public ResponseEntity<EventResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody EventRequest request) {
        var userId = AuthenticatedUser.id(jwt);
        prepareGoogle(userId, events.googleAccountForSource(userId, request.sourceId()));
        return WriteCommandResponses.of(commands.send(CreateEventHandler.TYPE, userId, idempotencyKey, request,
                EventResponse.class));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String eventId,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody EventRequest request) {
        var userId = AuthenticatedUser.id(jwt);
        prepareGoogle(userId, events.googleAccountForEvent(userId, eventId));
        return WriteCommandResponses.of(commands.send(UpdateEventHandler.TYPE, userId, idempotencyKey,
                new UpdateEventCommand(eventId, request), EventResponse.class));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String eventId,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        var userId = AuthenticatedUser.id(jwt);
        prepareGoogle(userId, events.googleAccountForEvent(userId, eventId));
        return WriteCommandResponses.of(commands.send(DeleteEventHandler.TYPE, userId, idempotencyKey,
                new DeleteEventCommand(eventId), Void.class));
    }

    private void prepareGoogle(UUID userId, Optional<CalendarAccount> googleAccount) {
        googleAccount.ifPresent(account -> googleTokens.ensure(userId, account));
    }
}
