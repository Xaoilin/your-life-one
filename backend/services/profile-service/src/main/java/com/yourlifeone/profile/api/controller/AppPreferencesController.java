package com.yourlifeone.profile.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.profile.api.command.UpdateAppPreferencesHandler;
import com.yourlifeone.profile.api.mapper.AppPreferencesMapper;
import com.yourlifeone.profile.api.request.AppPreferencesRequest;
import com.yourlifeone.profile.api.response.AppPreferencesResponse;
import com.yourlifeone.profile.application.service.AppPreferencesService;
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

/** App preferences every device shares: theme, retention, telemetry, calendar view and goal tags. */
@RestController
@RequestMapping("/api/profile/v1/preferences")
@RequiredArgsConstructor
public class AppPreferencesController {
    private final AppPreferencesService preferences;
    private final AppPreferencesMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public AppPreferencesResponse get(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(preferences.get(AuthenticatedUser.id(jwt)));
    }

    @PutMapping
    public ResponseEntity<AppPreferencesResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody AppPreferencesRequest request) {
        return WriteCommandResponses.of(commands.send(UpdateAppPreferencesHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, AppPreferencesResponse.class));
    }
}
