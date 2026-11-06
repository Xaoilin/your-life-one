package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.prayer.api.command.SaveMomentumRemindersHandler;
import com.yourlifeone.prayer.api.command.SnoozeReminderCommand;
import com.yourlifeone.prayer.api.command.SnoozeReminderHandler;
import com.yourlifeone.prayer.api.mapper.ReminderResponseMapper;
import com.yourlifeone.prayer.api.request.MomentumRemindersRequest;
import com.yourlifeone.prayer.api.response.MomentumRemindersResponse;
import com.yourlifeone.prayer.api.response.ReminderResponse;
import com.yourlifeone.prayer.api.response.RemindersResponse;
import com.yourlifeone.prayer.application.service.ActiveReminderService;
import com.yourlifeone.prayer.application.service.MomentumReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reminders the service decided and sent (over the live-update stream), and the momentum preferences it
 * decides momentum prompts from. Writes go through the prayer command queue with an {@code Idempotency-Key}.
 */
@RestController
@RequestMapping("/api/prayer/v1/reminders")
@RequiredArgsConstructor
public class PrayerReminderController {
    private final ActiveReminderService active;
    private final MomentumReminderService momentum;
    private final ReminderResponseMapper responses;
    private final WriteCommandGateway commands;

    /** Reminders to show now (newest first) and every pillar's momentum reminder preference. */
    @GetMapping
    public RemindersResponse reminders(@AuthenticationPrincipal Jwt jwt) {
        var userId = AuthenticatedUser.id(jwt);
        return responses.toResponse(active.active(userId), momentum.preferences(userId));
    }

    /** Replaces the given pillars' momentum reminder preferences; answers with every pillar's. */
    @PutMapping("/momentum")
    public ResponseEntity<MomentumRemindersResponse> saveMomentum(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody MomentumRemindersRequest request) {
        return WriteCommandResponses.of(commands.send(SaveMomentumRemindersHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, MomentumRemindersResponse.class));
    }

    /**
     * Snoozes a sent reminder once; 404 {@code reminder_not_found}, 409 {@code snooze_used} or
     * {@code snooze_too_late}.
     */
    @PostMapping("/{key}/snooze")
    public ResponseEntity<ReminderResponse> snooze(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @PathVariable String key) {
        return WriteCommandResponses.of(commands.send(SnoozeReminderHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new SnoozeReminderCommand(key), ReminderResponse.class));
    }
}
