package com.yourlifeone.finance.ledger.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.finance.ledger.api.command.LedgerCommandHandlers;
import com.yourlifeone.finance.ledger.api.command.SaveAccountCommand;
import com.yourlifeone.finance.ledger.api.request.AccountRequest;
import com.yourlifeone.finance.ledger.api.response.AccountDeletedResponse;
import com.yourlifeone.finance.ledger.api.response.AccountResponse;
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

/** Accounts; saving one sets its balance as given. IDs are chosen by the app; writes go through the command queue. */
@RestController
@RequestMapping("/api/finance/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final WriteCommandGateway commands;

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody AccountRequest request) {
        return WriteCommandResponses.of(commands.send(LedgerCommandHandlers.SAVE_ACCOUNT, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveAccountCommand(id, request), AccountResponse.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AccountDeletedResponse> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(LedgerCommandHandlers.DELETE_ACCOUNT, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), AccountDeletedResponse.class));
    }
}
