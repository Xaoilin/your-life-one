package com.yourlifeone.planner.gamification.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.planner.gamification.api.command.MomentumCommandHandlers;
import com.yourlifeone.planner.gamification.api.command.SaveMomentumCommand;
import com.yourlifeone.planner.gamification.api.mapper.MomentumMapper;
import com.yourlifeone.planner.gamification.api.request.MomentumRequest;
import com.yourlifeone.planner.gamification.api.response.MomentumPillarResponse;
import com.yourlifeone.planner.gamification.api.response.MomentumResponse;
import com.yourlifeone.planner.gamification.application.service.MomentumService;
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

/** Daily momentum (learn and move): the app's state for each pillar, saved whole with its version. */
@RestController
@RequestMapping("/api/planner/v1/momentum")
@RequiredArgsConstructor
public class MomentumController {
    private final MomentumService momentum;
    private final MomentumMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public MomentumResponse momentum(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toMomentumResponse(momentum.list(AuthenticatedUser.id(jwt)).values());
    }

    @PutMapping("/{pillar}")
    public ResponseEntity<MomentumPillarResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String pillar,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody MomentumRequest request) {
        return WriteCommandResponses.of(commands.send(MomentumCommandHandlers.SAVE_PILLAR, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveMomentumCommand(pillar, request), MomentumPillarResponse.class));
    }
}
