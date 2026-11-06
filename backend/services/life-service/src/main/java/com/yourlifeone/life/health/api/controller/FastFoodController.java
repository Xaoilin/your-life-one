package com.yourlifeone.life.health.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.life.health.api.command.DeleteFastFoodEntryCommand;
import com.yourlifeone.life.health.api.command.DeleteFastFoodEntryHandler;
import com.yourlifeone.life.health.api.command.SaveFastFoodEntryCommand;
import com.yourlifeone.life.health.api.command.SaveFastFoodEntryHandler;
import com.yourlifeone.life.health.api.mapper.FastFoodMapper;
import com.yourlifeone.life.health.api.request.FastFoodEntryRequest;
import com.yourlifeone.life.health.api.response.FastFoodEntriesResponse;
import com.yourlifeone.life.health.api.response.FastFoodEntryResponse;
import com.yourlifeone.life.health.application.service.FastFoodService;
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

/** The fast-food journal. Entry IDs are chosen by the app, so a retried save never duplicates an entry. */
@RestController
@RequestMapping("/api/life/v1/health/fast-food")
@RequiredArgsConstructor
public class FastFoodController {
    private final FastFoodService entries;
    private final FastFoodMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public FastFoodEntriesResponse list(@AuthenticationPrincipal Jwt jwt) {
        return new FastFoodEntriesResponse(mapper.toResponses(entries.list(AuthenticatedUser.id(jwt))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FastFoodEntryResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody FastFoodEntryRequest request) {
        return WriteCommandResponses.of(commands.send(SaveFastFoodEntryHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveFastFoodEntryCommand(id, request), FastFoodEntryResponse.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(DeleteFastFoodEntryHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new DeleteFastFoodEntryCommand(id), Void.class));
    }
}
