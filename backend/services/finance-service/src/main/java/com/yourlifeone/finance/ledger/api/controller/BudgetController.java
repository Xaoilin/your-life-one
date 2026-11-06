package com.yourlifeone.finance.ledger.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.finance.api.response.DeletedResponse;
import com.yourlifeone.finance.ledger.api.command.LedgerCommandHandlers;
import com.yourlifeone.finance.ledger.api.command.SaveBudgetCommand;
import com.yourlifeone.finance.ledger.api.request.BudgetRequest;
import com.yourlifeone.finance.ledger.api.response.BudgetResponse;
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

/** Monthly budgets, one per expense category. IDs are chosen by the app; writes go through the command queue. */
@RestController
@RequestMapping("/api/finance/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final WriteCommandGateway commands;

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody BudgetRequest request) {
        return WriteCommandResponses.of(commands.send(LedgerCommandHandlers.SAVE_BUDGET, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveBudgetCommand(id, request), BudgetResponse.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeletedResponse> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(LedgerCommandHandlers.DELETE_BUDGET, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), DeletedResponse.class));
    }
}
