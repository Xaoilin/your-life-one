package com.yourlifeone.knowledge.lifestyle.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.knowledge.lifestyle.api.command.LifestyleCommandHandlers;
import com.yourlifeone.knowledge.lifestyle.api.command.MoveItemCommand;
import com.yourlifeone.knowledge.lifestyle.api.command.SaveItemCommand;
import com.yourlifeone.knowledge.lifestyle.api.mapper.LifestyleMapper;
import com.yourlifeone.knowledge.lifestyle.api.request.LifestyleItemRequest;
import com.yourlifeone.knowledge.lifestyle.api.request.MoveItemRequest;
import com.yourlifeone.knowledge.lifestyle.api.request.ReorderColumnRequest;
import com.yourlifeone.knowledge.lifestyle.api.response.LifestyleItemResponse;
import com.yourlifeone.knowledge.lifestyle.api.response.LifestyleItemsResponse;
import com.yourlifeone.knowledge.lifestyle.application.service.LifestyleService;
import com.yourlifeone.knowledge.shared.IdCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The lifestyle tracker. Item IDs are chosen by the app, so a retried save never duplicates an item. */
@RestController
@RequestMapping("/api/knowledge/v1/lifestyle")
@RequiredArgsConstructor
public class LifestyleController {
    private final LifestyleService lifestyle;
    private final LifestyleMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public LifestyleItemsResponse items(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toItemsResponse(lifestyle.list(AuthenticatedUser.id(jwt)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<LifestyleItemResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody LifestyleItemRequest request) {
        return WriteCommandResponses.of(commands.send(LifestyleCommandHandlers.SAVE_ITEM, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveItemCommand(id, request), LifestyleItemResponse.class));
    }

    @PostMapping("/items/{id}/move")
    public ResponseEntity<LifestyleItemsResponse> move(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody MoveItemRequest request) {
        return WriteCommandResponses.of(commands.send(LifestyleCommandHandlers.MOVE_ITEM, AuthenticatedUser.id(jwt),
                idempotencyKey, new MoveItemCommand(id, request), LifestyleItemsResponse.class));
    }

    @PostMapping("/order")
    public ResponseEntity<LifestyleItemsResponse> reorder(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ReorderColumnRequest request) {
        return WriteCommandResponses.of(commands.send(LifestyleCommandHandlers.REORDER_COLUMN, AuthenticatedUser.id(jwt),
                idempotencyKey, request, LifestyleItemsResponse.class));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(LifestyleCommandHandlers.DELETE_ITEM, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), Void.class));
    }
}
