package com.yourlifeone.profile.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.profile.api.command.SaveViewPreferenceCommand;
import com.yourlifeone.profile.api.command.SaveViewPreferenceHandler;
import com.yourlifeone.profile.api.request.ViewPreferenceRequest;
import com.yourlifeone.profile.api.response.ViewPreferenceResponse;
import com.yourlifeone.profile.api.response.ViewPreferencesResponse;
import com.yourlifeone.profile.application.service.ViewPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/profile/v1/view-preferences")
@RequiredArgsConstructor
public class ViewPreferencesController {
    private final ViewPreferencesService preferences;
    private final WriteCommandGateway commands;

    @GetMapping
    public ResponseEntity<ViewPreferencesResponse> get(@AuthenticationPrincipal Jwt jwt) {
        requireFirstParty(jwt);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ViewPreferencesResponse(preferences.values(AuthenticatedUser.id(jwt))));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ViewPreferenceResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String key,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ViewPreferenceRequest request) {
        requireFirstParty(jwt);
        var command = new SaveViewPreferenceCommand(key, request.value());
        return WriteCommandResponses.of(commands.send(SaveViewPreferenceHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, command, ViewPreferenceResponse.class));
    }

    /** This feature has no approved external-agent domain yet; another domain's token grants no access. */
    private static void requireFirstParty(Jwt jwt) {
        var clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && !clientId.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "agent_not_approved", "Display-preference agent access is not available.");
        }
    }
}
