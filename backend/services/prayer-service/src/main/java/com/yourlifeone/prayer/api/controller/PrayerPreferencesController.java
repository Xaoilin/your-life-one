package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.prayer.api.command.UpdatePreferencesHandler;
import com.yourlifeone.prayer.api.mapper.PrayerResponseMapper;
import com.yourlifeone.prayer.api.request.PreferencesRequest;
import com.yourlifeone.prayer.api.response.PreferencesResponse;
import com.yourlifeone.prayer.application.service.PrayerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Prayer preferences; saving goes through the prayer command queue. */
@RestController
@RequestMapping("/api/prayer/v1/preferences")
@RequiredArgsConstructor
public class PrayerPreferencesController {
    private final PrayerProfileService profiles;
    private final PrayerResponseMapper responses;
    private final WriteCommandGateway commands;

    @GetMapping
    public PreferencesResponse get(@AuthenticationPrincipal Jwt jwt) {
        return responses.toPreferences(profiles.getOrCreate(AuthenticatedUser.id(jwt)));
    }

    @PutMapping
    public ResponseEntity<PreferencesResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody PreferencesRequest request) {
        return WriteCommandResponses.of(commands.send(UpdatePreferencesHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, PreferencesResponse.class));
    }
}
