package com.yourlifeone.profile.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.profile.api.command.SaveIntegrationCommand;
import com.yourlifeone.profile.api.command.SaveIntegrationHandler;
import com.yourlifeone.profile.api.mapper.IntegrationResponseMapper;
import com.yourlifeone.profile.api.request.IntegrationRequest;
import com.yourlifeone.profile.api.response.IntegrationResponse;
import com.yourlifeone.profile.api.response.IntegrationsResponse;
import com.yourlifeone.profile.application.service.IntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The user's integration connection records, one per provider. */
@RestController
@RequestMapping("/api/profile/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {
    private final IntegrationService integrations;
    private final IntegrationResponseMapper responses;
    private final WriteCommandGateway commands;

    @GetMapping
    public IntegrationsResponse list(@AuthenticationPrincipal Jwt jwt) {
        return new IntegrationsResponse(responses.toResponses(integrations.list(AuthenticatedUser.id(jwt))));
    }

    @PutMapping("/{provider}")
    public ResponseEntity<IntegrationResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String provider,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody IntegrationRequest request) {
        var command = new SaveIntegrationCommand(provider, request.status(), request.configuredAt(), request.lastError());
        return WriteCommandResponses.of(commands.send(SaveIntegrationHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, command, IntegrationResponse.class));
    }
}
