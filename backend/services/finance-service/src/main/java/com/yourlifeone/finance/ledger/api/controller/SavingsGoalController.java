package com.yourlifeone.finance.ledger.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.finance.api.response.DeletedResponse;
import com.yourlifeone.finance.ledger.api.command.LedgerCommandHandlers;
import com.yourlifeone.finance.ledger.api.command.SaveSavingsGoalCommand;
import com.yourlifeone.finance.ledger.api.request.SavingsGoalRequest;
import com.yourlifeone.finance.ledger.api.response.SavingsGoalResponse;
import com.yourlifeone.finance.shared.IdCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Savings goals; the completion time is stamped by the service. IDs are chosen by the app; writes go through the command queue. */
@RestController
@RequestMapping("/api/finance/v1/savings-goals")
@RequiredArgsConstructor
public class SavingsGoalController {
    private final WriteCommandGateway commands;

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SavingsGoalRequest request) {
        return WriteCommandResponses.of(commands.send(LedgerCommandHandlers.SAVE_SAVINGS_GOAL, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveSavingsGoalCommand(id, request), SavingsGoalResponse.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeletedResponse> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(LedgerCommandHandlers.DELETE_SAVINGS_GOAL, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), DeletedResponse.class));
    }
}
