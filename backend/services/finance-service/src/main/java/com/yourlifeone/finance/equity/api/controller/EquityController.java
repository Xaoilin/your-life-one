package com.yourlifeone.finance.equity.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.finance.api.response.DeletedResponse;
import com.yourlifeone.finance.equity.api.command.DeletePositionCommand;
import com.yourlifeone.finance.equity.api.command.EquityCommandHandlers;
import com.yourlifeone.finance.equity.api.command.SavePositionCommand;
import com.yourlifeone.finance.equity.api.mapper.EquityMapper;
import com.yourlifeone.finance.equity.api.request.SavePositionRequest;
import com.yourlifeone.finance.equity.api.response.PositionResponse;
import com.yourlifeone.finance.equity.api.response.PositionsResponse;
import com.yourlifeone.finance.equity.application.service.EquityService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Private equity planning records; AI agents approved for Equity may use them. */
@RestController
@RequestMapping("/api/finance/v1/equity/positions")
@RequiredArgsConstructor
public class EquityController {
    private final EquityService equity;
    private final EquityMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public PositionsResponse positions(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return mapper.toResponse(equity.search(AuthenticatedUser.id(jwt), query, limit, offset));
    }

    @GetMapping("/{id}")
    public PositionResponse position(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return mapper.toResponse(equity.position(AuthenticatedUser.id(jwt), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PositionResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SavePositionRequest request) {
        return WriteCommandResponses.of(commands.send(EquityCommandHandlers.SAVE_POSITION, AuthenticatedUser.id(jwt),
                idempotencyKey, new SavePositionCommand(id, request), PositionResponse.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeletedResponse> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestParam String expectedUpdatedAt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(EquityCommandHandlers.DELETE_POSITION, AuthenticatedUser.id(jwt),
                idempotencyKey, new DeletePositionCommand(id, expectedUpdatedAt), DeletedResponse.class));
    }
}
